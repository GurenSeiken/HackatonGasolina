package com.gurenseiken.gas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.preference.PreferenceManager

class RecuperarSesion : AppCompatActivity() {

    //nombre de la llave para crear los datos del preference manager
    private val key ="MY_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_sesion)

        //crear el preference manager para poder usarlo dentro del script
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val recButton = findViewById<Button>(R.id.RecuperarButton)
        val placa = findViewById<EditText>(R.id.textPlaca)

        recButton.setOnClickListener(){

            //guarda el valor de la placa en el preference manager para usarlo directamente si ya tiene un usuario creado
            val editor= prefs.edit()
            editor.putString(key,placa.text.toString())
            editor.apply()
            ChangeScreen()
        }

    }
    //funcion para cambiar de activity
    private fun ChangeScreen(){
        val pantalla2 = Intent(this,MainActivity::class.java)
        startActivity(pantalla2)
    }
}