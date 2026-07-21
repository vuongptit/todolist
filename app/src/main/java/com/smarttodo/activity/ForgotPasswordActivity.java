package com.smarttodo.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.smarttodo.databinding.ActivityForgotPasswordBinding;
import com.smarttodo.utils.ValidationUtils;
import com.smarttodo.viewmodel.AuthViewModel;

/**
 * ForgotPasswordActivity - màn hình quên mật khẩu.
 * Gửi email reset password qua Firebase Authentication.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        authViewModel.isLoading.observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSendReset.setEnabled(!isLoading);
        });

        authViewModel.successMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(com.smarttodo.R.color.primary, null))
                        .show();
                // Delay rồi finish
                binding.getRoot().postDelayed(this::finish, 3000);
            }
        });

        authViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                authViewModel.clearError();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
        binding.btnSendReset.setOnClickListener(v -> attemptSendReset());
    }

    private void attemptSendReset() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";

        String emailError = ValidationUtils.validateEmail(email);
        binding.tilEmail.setError(emailError);

        if (emailError != null) return;

        authViewModel.sendPasswordReset(email);
    }
}
