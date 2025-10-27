package com.sanna.provcalapp.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sanna.provcalapp.MainActivity
import com.sanna.provcalapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Botón Ingresar → Ir directo al Home
        binding.btnLogin.setOnClickListener {
            goToMain()
        }

        // 🔹 Por ahora solo mostramos mensaje cuando hacen clic en "¿Olvidaste tu contraseña?"
        binding.tvForgot.setOnClickListener {
            // TODO: Ir a pantalla de recuperación de clave cuando esté implementado
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
