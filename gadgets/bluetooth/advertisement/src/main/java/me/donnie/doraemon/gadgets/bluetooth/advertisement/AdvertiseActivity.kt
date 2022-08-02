package me.donnie.doraemon.gadgets.bluetooth.advertisement

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import me.donnie.doraemon.gadgets.bluetooth.advertisement.databinding.ActivityAdvertiseBinding

class AdvertiseActivity : AppCompatActivity() {

	private lateinit var binding: ActivityAdvertiseBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityAdvertiseBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.button.setOnClickListener {
			Toast.makeText(this, "show toast", Toast.LENGTH_LONG).show()
			finish()
		}
	}
}