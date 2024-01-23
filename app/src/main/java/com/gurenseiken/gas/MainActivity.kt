package com.gurenseiken.gas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import com.google.android.gms.location.LocationRequest
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks{
    private val db= FirebaseFirestore.getInstance()
    private lateinit var kmh: TextView
    private lateinit var textkm: TextView
    private lateinit var KmActualText : TextView
    private var Plac =""
    private var KMActual = 0f
    private var KMActualT = ""
    private val key ="MY_KEY"
    private val key2="MY_KEY_2"
    private var kmOffline=0f
    private var distanciaCalc=0f
    private var t1=0L
    private var t2=0L
    private var deltaT=0f

    private lateinit  var handler : Handler
    var delay = 10000
    var calckm =0
    var kmrecorrido=0f
    var fin=0f
    var ini=0f
    var del=0f
    var FECHAQUENOSEPUEDECAMBIAR=""



    private val LOCATION_PERM=124
    private var tiempoPromedio=0f
    private var veLocidadPromedio=1f
    private var lon=0f
    private var lat=0f




    //(gasolina gastada/km recorridos) * 100
    //esa es la formula para el gasto de gasolina


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var textoVelocidad : TextView
    private lateinit var textoKmNuevos : TextView


    private var isDone:Boolean by Delegates.observable(false){
        property, oldValue, newValue ->
        if(newValue==true){

            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }
    private fun runCot(){
        handler.postDelayed(myRunnble,delay.toLong())
    }

    val myRunnble : Runnable = object :Runnable{
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            trazabilidad(kmOffline, lat, lon)
            println("se enviaron datos a la base de datos")
            handler.postDelayed(this,delay.toLong())
        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handler = Handler(Looper.getMainLooper())
        runCot()


        textoVelocidad=findViewById(R.id.KMPHINRT)
        textoKmNuevos=findViewById(R.id.KmRecorridosNew)


        //
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        askForLocationPermission()
        createLocationRequest()

        locationCallback = object :LocationCallback(){
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                if(!isDone){
                    var speedToInt = locationResult.lastLocation?.speed
                    if (speedToInt != null) {
                        speedToInt=(speedToInt.toFloat()*3.6f)
                    }
                    textoVelocidad.text ="Velocidad: "+(speedToInt!!.toInt()).toString()+"Km/h"
                    if(speedToInt!! >=5f){

                        kmOffline+=distanciaCalc
                        veLocidadPromedio=(speedToInt+veLocidadPromedio)/2
                        kmh.text = "velocidad promedio: "+veLocidadPromedio.toString()+" Km/h"
                        kmOffline+= veLocidadPromedio*0.0002777778f

                        textoKmNuevos.text = "Nuevos kilometros a tu vehiculo: "+ (kmOffline.toString()) + " Km"


                    }else if(speedToInt<5f){
                        veLocidadPromedio =1f
                        tiempoPromedio =1f
                        kmh.text = "velocidad promedio: "+veLocidadPromedio.toString()+" Km/h"
                    }

                    lon = locationResult.lastLocation?.longitude?.toFloat()!!
                    lat = locationResult.lastLocation?.latitude?.toFloat()!!
                }
            }
        }




        //

        val pref = PreferenceManager.getDefaultSharedPreferences(this)

        // Obtiene el valor guardado en el preference manager de la placa
        Plac = pref.getString(key,"empty").toString()
        KMActualT = pref.getString(key2,"0").toString()

        KMActual = KMActual.toFloat()

        getDatos()
        //cargar()

        textkm = findViewById(R.id.TextoKLM)
        textkm.text = "Tu placa es: "+Plac

        KmActualText = findViewById(R.id.KmActual)

        KmActualText.text = KMActual.toString()


        kmh= findViewById(R.id.textView2)

        /*setUpSensorStuff()*/

        val editor= pref.edit()
        editor.putString(key2,KMActual.toString())
        editor.apply()

        val cerrarButton = findViewById<Button>(R.id.CerrarSesion)

        cerrarButton.setOnClickListener(){
            val editor = pref.edit()
            editor.remove(key)
            editor.apply()
            val pantalla2 = Intent(this,Formulario::class.java)
            startActivity(pantalla2)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun trazabilidad(km:Float, lat:Float, lon:Float,){
        FECHAQUENOSEPUEDECAMBIAR=LocalDateTime.now().toString()
        db.collection("Trazabilidad").document(Plac).
        collection(FECHAQUENOSEPUEDECAMBIAR).
        document(LocalDateTime.now().toString()).
        set(
            hashMapOf("placa" to Plac,
                "kmrecorridos" to km,
                "latitud" to lat,
                "longitud" to lon)
        )
        var calckm = kmOffline-calckm

        var del=ini-kmOffline
        KMActual= (KMActual+calckm).toFloat()
        db.collection("DatosUsuario").document(Plac).update(mapOf("Kilometraje" to KMActual.toDouble()))
        db.collection("Recorrido").document(FECHAQUENOSEPUEDECAMBIAR).set(hashMapOf(
            "placa" to Plac,
            "kmrecorridos" to del,
            "latitud" to lat,
            "longitud" to lon)
        )

    }
    private fun getDatos(){
        db.collection("DatosUsuario").document(Plac.toString()).get().addOnSuccessListener{
            KmActualText.text = (it.get("Kilometraje") as Double).toString()
            KMActual = (it.get("Kilometraje") as Double).toFloat()
        }
    }
    private fun getKmAnterior(){

        db.collection("Recorrido").document(FECHAQUENOSEPUEDECAMBIAR).get().addOnSuccessListener {
            ini= (it.get("kmrecorridos") as Float)
        }
    }

    private fun askForLocationPermission(){
        if(hasLocationPermissions()){
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            //do nothing
            }
        }else{
            EasyPermissions.requestPermissions(this,
                "Need permisison for use the aplication",
                LOCATION_PERM,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    private fun hasLocationPermissions():Boolean{
        return EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }
    fun createLocationRequest(){
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000L).build()
    }



    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onPause() {
        super.onPause()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE){
            val yes="Allow"
            val no="Deny"
            Toast.makeText(this,"onActivityResult",Toast.LENGTH_LONG).show()
        }
    }



    // las funciones de abajo son para poder heredarlas y sha

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        //por ahora no es relevante
    }

    override fun onRationaleAccepted(requestCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onRationaleDenied(requestCode: Int) {
        TODO("Not yet implemented")
    }



}

private fun Handler.postDelayed(locationCallback: LocationCallback, l: Long) {

}
