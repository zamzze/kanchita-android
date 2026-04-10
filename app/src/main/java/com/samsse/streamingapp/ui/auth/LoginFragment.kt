package com.samsse.streamingapp.ui.auth

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.samsse.streamingapp.R
import com.samsse.streamingapp.databinding.FragmentLoginBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModel()

    private var isLoginMode = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupListeners()
        observeState()
        binding.etEmail.requestFocus()
    }

    private fun setupTabs() {
        binding.btnTabLogin.setOnClickListener {
            if (!isLoginMode) switchMode(login = true)
        }
        binding.btnTabRegister.setOnClickListener {
            if (isLoginMode) switchMode(login = false)
        }
        updateTabUI()
    }

    private fun switchMode(login: Boolean) {
        isLoginMode = login
        binding.tvError.isVisible = false
        binding.etEmail.text?.clear()
        binding.etPassword.text?.clear()
        binding.etConfirmPassword.text?.clear()
        updateTabUI()
        binding.etEmail.requestFocus()
    }

    private fun updateTabUI() {
        if (isLoginMode) {
            binding.btnTabLogin.alpha    = 1f
            binding.btnTabRegister.alpha = 0.5f
            binding.layoutConfirmPassword.isVisible = false
            binding.btnAction.text = "Entrar"
        } else {
            binding.btnTabLogin.alpha    = 0.5f
            binding.btnTabRegister.alpha = 1f
            binding.layoutConfirmPassword.isVisible = true
            binding.btnAction.text = "Registrarse"
        }
    }

    private fun setupListeners() {
        binding.btnAction.setOnClickListener { doAction() }

        binding.etPassword.setOnEditorActionListener { _, actionId, event ->
            val isDone = actionId == EditorInfo.IME_ACTION_DONE ||
                    event?.keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            if (isDone && isLoginMode) {
                doAction()
                true
            } else false
        }

        binding.etConfirmPassword.setOnEditorActionListener { _, actionId, event ->
            val isDone = actionId == EditorInfo.IME_ACTION_DONE ||
                    event?.keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            if (isDone && !isLoginMode) {
                doAction()
                true
            } else false
        }
    }

    private fun doAction() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        if (isLoginMode) {
            viewModel.login(email, password)
        } else {
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            viewModel.register(email, password, confirmPassword)
        }
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginViewModel.AuthState.Idle -> {
                    binding.progressBar.isVisible = false
                    binding.btnAction.isEnabled   = true
                }
                is LoginViewModel.AuthState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnAction.isEnabled   = false
                    binding.tvError.isVisible     = false
                }
                is LoginViewModel.AuthState.Success -> {
                    findNavController().navigate(R.id.action_login_to_home)
                }
                is LoginViewModel.AuthState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnAction.isEnabled   = true
                    binding.tvError.text          = state.message
                    binding.tvError.isVisible     = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}