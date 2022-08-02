package me.donnie.doraemon.gadgets.bluetooth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import me.donnie.doraemon.gadgets.bluetooth.advertisement.AdvertiseActivity
import me.donnie.doraemon.gadgets.bluetooth.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.mainText.setOnClickListener {
			startActivity(Intent(this, AdvertiseActivity::class.java))
		}
	}
}