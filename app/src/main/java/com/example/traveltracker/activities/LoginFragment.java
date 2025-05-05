package com.example.traveltracker.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.traveltracker.R;
import com.example.traveltracker.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout via View Binding
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        // Set listeners using View Binding
        binding.buttonLogin.setOnClickListener(v -> loginUser());
        binding.buttonSignUp.setOnClickListener(v -> signUpUser());

        Log.d(TAG, "LoginFragment view created.");
    }

    private void loginUser() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }
        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        Toast.makeText(getContext(), "Login Successful.", Toast.LENGTH_SHORT).show();
                        // Navigate using NavController action defined in nav_graph.xml
                        navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        String errorMessage = "Authentication failed.";
                        if (task.getException() != null) {
                            errorMessage += " Check email/password.";
                            Log.e(TAG, "Login Error: " + task.getException().getMessage());
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signUpUser() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }
        if (password.length() < 6) {
            binding.textFieldPassword.setError("Password must be at least 6 characters");
            binding.editTextPassword.requestFocus();
            return;
        }
        showLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        Toast.makeText(getContext(), "Sign Up Successful! Welcome.", Toast.LENGTH_SHORT).show();
                        navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = "Sign Up failed.";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "This email address is already registered. Please Login.";
                            binding.textFieldEmail.setError(errorMessage);
                            binding.editTextEmail.requestFocus();
                        } else if (task.getException() != null) {
                            if (task.getException().getMessage() != null && task.getException().getMessage().contains("CONFIGURATION_NOT_FOUND")) {
                                errorMessage = "Sign Up failed due to internal configuration error. Please check setup.";
                                Log.e(TAG, "CONFIGURATION_NOT_FOUND error during sign up. Check App Check setup.");
                            } else {
                                errorMessage += " " + task.getException().getMessage();
                            }
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        binding.textFieldEmail.setError(null);
        binding.textFieldPassword.setError(null);
        boolean isValid = true;
        if (TextUtils.isEmpty(email)) {
            binding.textFieldEmail.setError("Email is required.");
            binding.editTextEmail.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textFieldEmail.setError("Enter a valid email address.");
            binding.editTextEmail.requestFocus();
            isValid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.textFieldPassword.setError("Password is required.");
            if (isValid) { binding.editTextPassword.requestFocus(); }
            isValid = false;
        }
        return isValid;
    }

    private void showLoading(boolean isLoading) {
        if (binding == null) return;
        binding.progressBarLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setEnabled(!isLoading);
        binding.buttonSignUp.setEnabled(!isLoading);
        binding.editTextEmail.setEnabled(!isLoading);
        binding.editTextPassword.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}