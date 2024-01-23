package com.gurenseiken.gas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime

class Formulario : AppCompatActivity() {

    private val db= FirebaseFirestore.getInstance()
    private val key ="MY_KEY"
    private var Plac =""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        Plac = prefs.getString(key,"empty").toString()

        if(Plac!="empty"){
            ChangeScreen()
        }

        //get preference manager
        //val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario)
        val bundle = intent.extras
        val tr=bundle?.getString("Placa","empty")

        val crearBut = findViewById<Button>(R.id.CrearButton)
        val gotoRecuperar = findViewById<Button>(R.id.GoToRecuperar)

        crearBut.setOnClickListener{
            val placa =findViewById<EditText>(R.id.Placa)
            val marca = findViewById<EditText>(R.id.Marca)
            val modelo = findViewById<EditText>(R.id.Modelo)
            val cilindrada = findViewById<EditText>(R.id.Cilindrada)
            val numCilindros = findViewById<EditText>(R.id.Cilindros)
            val kilometraje = findViewById<EditText>(R.id.Kilometraje)
            val tanque = findViewById<EditText>(R.id.Tanque)


            db.collection("DatosVehiculo").document(placa.text.toString()).set(
                hashMapOf("Placa" to placa.text.toString(),
                    "Marca" to marca.text.toString(),
                    "Modelo" to modelo.text.toString().toInt())
            )
            db.collection("DatosUsuario").document(placa.text.toString()).set(
                hashMapOf("Cilindrada" to cilindrada.text.toString().toInt(),
                    "Kilometraje" to kilometraje.text.toString().toFloat(),
                    "NumeroCilindros" to numCilindros.text.toString().toInt(),
                    "Placa" to placa.text.toString(),
                    "TamañoDelTanqueEnGalones" to tanque.text.toString().toFloat()
                    )
            )
            db.collection("Trazabilidad").document(placa.text.toString()).
            collection(LocalDateTime.now().toString()).
            document(LocalDateTime.now().toString()).
            set(
                hashMapOf("kmrecorridos" to 0,
                    "latitud" to 0,
                    "longitud" to 0)
            )
            val editor= prefs.edit()
            editor.putString(key,placa.text.toString())
            editor.apply()
            ChangeScreen()
        }

        gotoRecuperar.setOnClickListener(){
            val pantalla2 = Intent(this,RecuperarSesion::class.java)
            startActivity(pantalla2)
        }
        /*fun getDatos(){
            db.collection("DatosVehiculo").document(placa.text.toString()).get().addOnSuccessListener{
            }
        }*/

    }
    private fun guardarPlaca(){

    }
    private fun ChangeScreen(){
        val pantalla2 = Intent(this,MainActivity::class.java)
        startActivity(pantalla2)
    }


}