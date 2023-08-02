package com.example.clicker.presentation.stream

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import com.example.clicker.R
import com.example.clicker.databinding.FragmentHomeBinding
import com.example.clicker.databinding.FragmentStreamBinding
import androidx.fragment.app.activityViewModels


/**
 * A simple [Fragment] subclass.
 * Use the [StreamFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StreamFragment : Fragment() {

    private var _binding: FragmentStreamBinding? = null
    private val binding get() = _binding!!
    private val streamViewModel: StreamViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentStreamBinding.inflate(inflater, container, false)
        //val channelName =streamViewModel.channelName.value

        val channelName = streamViewModel.channelName.value!!
        Log.d("twitchNameonCreateView",channelName)

        val url="https://player.twitch.tv/?channel=$channelName&muted=false&controls=false&parent=modderz"

       // val view = binding.root

      val view =  setOrientation(
            resources = resources,
            binding = binding,
            streamViewModel = streamViewModel
        )

        val myWebView: WebView = view.findViewById(R.id.webView)

        setWebView(
            myWebView = myWebView,
            url = url
        )

        return view
    }


}

fun setOrientation(
    resources:Resources,
    binding: FragmentStreamBinding,
    streamViewModel: StreamViewModel
): FrameLayout {
    if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
        binding.composeView.apply{
            setContent {
                StreamView(
                    streamViewModel
                )
            }
        }
    }
    return binding.root
}

fun setWebView(
    myWebView: WebView,
    url:String
){
    myWebView.settings.mediaPlaybackRequiresUserGesture = false

    myWebView.settings.javaScriptEnabled = true
    myWebView.isClickable = true
    myWebView.settings.domStorageEnabled = true; //THIS ALLOWS THE US TO CLICK ON THE MATURE AUDIENCE BUTTON

    myWebView.settings.allowContentAccess = true;
    myWebView.settings.allowFileAccess = true;

    myWebView.settings.setSupportZoom(true);

    myWebView.loadUrl(url)
}