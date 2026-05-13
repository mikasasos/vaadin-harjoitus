package com.example.application.views.register;

import com.example.application.data.Role;
import com.example.application.data.SampleBook;
import com.example.application.data.User;
import com.example.application.services.UserService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@AnonymousAllowed
@PageTitle("Registration")
@Route(value = "register", layout = MainLayout.class)
public class RegistrationView extends VerticalLayout {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;


    private TextField username = new TextField("Username");
    private TextField name = new TextField("Full Name");
    private PasswordField password = new PasswordField("Password");
    private PasswordField confirmPassword = new PasswordField("Confirm Password");
    private MemoryBuffer buffer = new MemoryBuffer();
    private Upload upload = new Upload(buffer);
    private Image imagePreview = new Image();

    private BeanValidationBinder<User> binder = new BeanValidationBinder<>(User.class);
    private User user = new User();
    private byte[] profilePictureData;

    public RegistrationView(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setWidth("500px");
        formContainer.setPadding(true);

        H2 title = new H2("Create Account");

        FormLayout formLayout = new FormLayout();

        username.setRequired(true);
        username.setRequiredIndicatorVisible(true);

        name.setRequired(true);
        name.setRequiredIndicatorVisible(true);

        password.setRequired(true);
        password.setRequiredIndicatorVisible(true);

        confirmPassword.setRequired(true);
        confirmPassword.setRequiredIndicatorVisible(true);

        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(1024 * 1024); // 1MB
        upload.addSucceededListener(event -> {
            try {
                InputStream inputStream = buffer.getInputStream();
                profilePictureData = inputStream.readAllBytes();
                Notification.show("Profile picture uploaded successfully", 3000, Notification.Position.BOTTOM_START);
            } catch (IOException e) {
                Notification.show("Error uploading file: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        binder.forField(username)
                .asRequired("Username is required")
                .bind(User::getUsername, User::setUsername);

        binder.forField(name)
                .asRequired("Full name is required")
                .bind(User::getName, User::setName);


        Button registerButton = new Button("Register", event -> register());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button loginButton = new Button("Already have an account? Login", event ->
                getUI().ifPresent(ui -> ui.navigate("login")));
        loginButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        formLayout.add(username, name, password, confirmPassword);
        formLayout.setColspan(username, 2);
        formLayout.setColspan(name, 2);
        formLayout.setColspan(password, 2);
        formLayout.setColspan(confirmPassword, 2);

        formContainer.add(title, formLayout, upload, registerButton, loginButton);

        add(formContainer);
    }

    private void register() {
        if (!password.getValue().equals(confirmPassword.getValue())) {
            Notification.show("Passwords do not match", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (password.getValue().isEmpty()) {
            Notification.show("Password is required", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            binder.writeBean(user);
            user.setHashedPassword(passwordEncoder.encode(password.getValue()));
            user.setRoles(Set.of(Role.USER));
            user.setProfilePicture(profilePictureData);

            userService.save(user);

            Notification.show("Registration successful! Please login.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate("login"));

        } catch (ValidationException e) {
            Notification.show("Please fill all required fields correctly", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

