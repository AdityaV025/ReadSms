package com.android.readsms

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.android.readsms.databinding.FragmentReadSmsBinding

/**
Created By Aditya Verma on 18/10/21
 **/

class ReadSmsFragment : Fragment(R.layout.fragment_read_sms), CoroutineScope {

    private var _binding: FragmentReadSmsBinding? = null
    private val binding get() = _binding!!

    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private var permissionGiven = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + SupervisorJob()

    private var messageCount: MutableLiveData<Int> = MutableLiveData()

    private val noOfMessage: LiveData<Int>
        get() { return messageCount }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentReadSmsBinding.bind(view)
        noOfMessage.observe(viewLifecycleOwner, ::setNoOfMessage)

        binding.apply {
            getSmsBtn.setOnClickListener {
                if (isPermissionGiven()) {
                    val numOfDays = numberOfDays.text.toString()
                    val phoneNum = phoneNumber.text.toString()

                    when {
                        phoneNum.isEmpty() -> {
                            Toast.makeText(requireContext(), "Phone Number cannot be empty", Toast.LENGTH_LONG).show()
                        }
                        numOfDays.isEmpty() -> {
                            Toast.makeText(requireContext(), "Number of Days cannot be empty", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            launch {
                                withContext(Dispatchers.IO) {
                                    messageCount.postValue(getNoOfMessage(numOfDays, phoneNum))
                                }
                            }

                        }
                    }
                } else
                    Toast.makeText(requireContext(), "Read SMS Permission Required!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestPermission()
    }

    /**
        Get the Num of Message from a particular phone Num and
        for the past N days via Content Resolver.
     **/
    private fun getNoOfMessage(noOfDay: String, number: String): Int {
        val msgCount: Int
        val contentResolver: ContentResolver = requireActivity().contentResolver
        val message = Uri.parse("content://sms/")
        val date: Long =
            Date(System.currentTimeMillis() - noOfDay.toLong() * 86400000L).time
        val c = contentResolver.query(
            message, arrayOf(
                "date"
            ), "address = '$number' AND date > ?", arrayOf("" + date), null
        )

        msgCount = c!!.count
        c.close()

        return msgCount
    }

    /**
        Set Num of Messages if the user has given Permission &
        and get the count of messages of a number for the past N days.
     **/
    private fun setNoOfMessage(count: Int) {
        binding.apply {
            messageText.visibility = View.VISIBLE
            if (count == 0) {
                messageText.text = R.string.sorry_msg.toString()
            } else {
                messageText.text =
                    count.toString().plus(" ").plus("number of messages found")
            }
        }
    }

    /**
        Request Read SMS Permission from the user.
     **/
    private fun requestPermission() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission())
        { permissionGiven = it }

        if (!permissionGiven)
            permissionLauncher.launch(Manifest.permission.READ_SMS)
    }

    private fun isPermissionGiven() : Boolean {
        if (!permissionGiven)
            permissionLauncher.launch(Manifest.permission.READ_SMS)

        return permissionGiven
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        coroutineContext.cancel()
    }

}