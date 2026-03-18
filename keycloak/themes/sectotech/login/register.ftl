<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>

    <#if section = "header">
        <h1 class="login-page-title">Criar Conta</h1>
        <p class="login-subtitle">Preencha os dados para se registrar</p>
    </#if>

    <#if section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">

            <div class="form-row">
                <div class="form-group">
                    <label for="firstName">Nome <span class="required">*</span></label>
                    <input
                        type="text"
                        id="firstName"
                        name="firstName"
                        value="${(register.formData.firstName!'')}"
                        placeholder="Ex: João"
                        aria-invalid="<#if messagesPerField.existsError('firstName')>true</#if>"
                    />
                    <#if messagesPerField.existsError('firstName')>
                        <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                            ${kcSanitize(messagesPerField.getFirstError('firstName'))?no_esc}
                        </span>
                    </#if>
                </div>

                <div class="form-group">
                    <label for="lastName">Sobrenome <span class="required">*</span></label>
                    <input
                        type="text"
                        id="lastName"
                        name="lastName"
                        value="${(register.formData.lastName!'')}"
                        placeholder="Ex: Silva"
                        aria-invalid="<#if messagesPerField.existsError('lastName')>true</#if>"
                    />
                    <#if messagesPerField.existsError('lastName')>
                        <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                            ${kcSanitize(messagesPerField.getFirstError('lastName'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="form-group">
                <label for="email">Email <span class="required">*</span></label>
                <input
                    type="email"
                    id="email"
                    name="email"
                    value="${(register.formData.email!'')}"
                    placeholder="email@exemplo.com"
                    autocomplete="email"
                    aria-invalid="<#if messagesPerField.existsError('email')>true</#if>"
                />
                <#if messagesPerField.existsError('email')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('email'))?no_esc}
                    </span>
                </#if>
            </div>

            <#if !realm.registrationEmailAsUsername>
                <div class="form-group">
                    <label for="username">Usuário <span class="required">*</span></label>
                    <input
                        type="text"
                        id="username"
                        name="username"
                        value="${(register.formData.username!'')}"
                        placeholder="Ex: joao.silva"
                        autocomplete="username"
                        aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                    />
                    <#if messagesPerField.existsError('username')>
                        <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                            ${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}
                        </span>
                    </#if>
                </div>
            </#if>

            <div class="form-group">
                <label for="password">Senha <span class="required">*</span></label>
                <input
                    type="password"
                    id="password"
                    name="password"
                    autocomplete="new-password"
                    placeholder="••••••••"
                    aria-invalid="<#if messagesPerField.existsError('password')>true</#if>"
                />
                <#if messagesPerField.existsError('password')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="form-group">
                <label for="password-confirm">Confirmar Senha <span class="required">*</span></label>
                <input
                    type="password"
                    id="password-confirm"
                    name="password-confirm"
                    autocomplete="new-password"
                    placeholder="••••••••"
                    aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                />
                <#if messagesPerField.existsError('password-confirm')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('password-confirm'))?no_esc}
                    </span>
                </#if>
            </div>

            <#if recaptchaRequired??>
                <div class="form-group">
                    <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                </div>
            </#if>

            <button type="submit" class="btn-brand">Cadastrar</button>
        </form>

        <script>
        (function() {
            var form = document.getElementById('kc-register-form');
            var firstNameInput = document.getElementById('firstName');
            var lastNameInput = document.getElementById('lastName');
            var emailInput = document.getElementById('email');
            var usernameInput = document.getElementById('username');
            var passwordInput = document.getElementById('password');
            var passwordConfirmInput = document.getElementById('password-confirm');

            // Regex patterns
            var nameRegex = /^[a-zA-ZÀ-ÿ\s'\-]+$/;
            var usernameRegex = /^[a-zA-Z0-9._\-]+$/;
            var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

            function removeValidationError(input) {
                var existing = input.parentNode.querySelector('.client-validation-error');
                if (existing) existing.remove();
                input.setAttribute('aria-invalid', 'false');
                input.style.borderColor = '';
            }

            function showValidationError(input, message) {
                removeValidationError(input);
                input.setAttribute('aria-invalid', 'true');
                input.style.borderColor = '#ef4444';
                var span = document.createElement('span');
                span.className = 'alert alert-error client-validation-error';
                span.style.marginTop = '0.25rem';
                span.style.display = 'block';
                span.style.padding = '0.5rem 0.75rem';
                span.style.fontSize = '0.8rem';
                span.textContent = message;
                input.parentNode.appendChild(span);
            }

            // --- Block invalid characters on keypress ---

            // Names: only letters, accented chars, spaces, hyphens, apostrophes
            function blockInvalidNameChars(e) {
                var char = String.fromCharCode(e.which || e.keyCode);
                if (e.ctrlKey || e.metaKey || e.which < 32) return;
                if (!/[a-zA-ZÀ-ÿ\s'\-]/.test(char)) {
                    e.preventDefault();
                }
            }

            // Names: sanitize paste
            function sanitizeNamePaste(e) {
                var pasted = (e.clipboardData || window.clipboardData).getData('text');
                var cleaned = pasted.replace(/[^a-zA-ZÀ-ÿ\s'\-]/g, '');
                if (cleaned !== pasted) {
                    e.preventDefault();
                    var start = this.selectionStart;
                    var end = this.selectionEnd;
                    var val = this.value;
                    this.value = val.substring(0, start) + cleaned + val.substring(end);
                    this.setSelectionRange(start + cleaned.length, start + cleaned.length);
                    this.dispatchEvent(new Event('input'));
                }
            }

            if (firstNameInput) {
                firstNameInput.addEventListener('keypress', blockInvalidNameChars);
                firstNameInput.addEventListener('paste', sanitizeNamePaste);
            }
            if (lastNameInput) {
                lastNameInput.addEventListener('keypress', blockInvalidNameChars);
                lastNameInput.addEventListener('paste', sanitizeNamePaste);
            }

            // Username: only letters, numbers, dot, hyphen, underscore
            if (usernameInput) {
                usernameInput.addEventListener('keypress', function(e) {
                    var char = String.fromCharCode(e.which || e.keyCode);
                    if (e.ctrlKey || e.metaKey || e.which < 32) return;
                    if (!/[a-zA-Z0-9._\-]/.test(char)) {
                        e.preventDefault();
                    }
                });
                usernameInput.addEventListener('paste', function(e) {
                    var pasted = (e.clipboardData || window.clipboardData).getData('text');
                    var cleaned = pasted.replace(/[^a-zA-Z0-9._\-]/g, '');
                    if (cleaned !== pasted) {
                        e.preventDefault();
                        var start = this.selectionStart;
                        var end = this.selectionEnd;
                        var val = this.value;
                        this.value = val.substring(0, start) + cleaned + val.substring(end);
                        this.setSelectionRange(start + cleaned.length, start + cleaned.length);
                        this.dispatchEvent(new Event('input'));
                    }
                });
            }

            // Email: block spaces
            if (emailInput) {
                emailInput.addEventListener('keypress', function(e) {
                    var char = String.fromCharCode(e.which || e.keyCode);
                    if (e.ctrlKey || e.metaKey || e.which < 32) return;
                    if (char === ' ') {
                        e.preventDefault();
                    }
                });
                emailInput.addEventListener('paste', function(e) {
                    var pasted = (e.clipboardData || window.clipboardData).getData('text');
                    var cleaned = pasted.replace(/\s/g, '');
                    if (cleaned !== pasted) {
                        e.preventDefault();
                        var start = this.selectionStart;
                        var end = this.selectionEnd;
                        var val = this.value;
                        this.value = val.substring(0, start) + cleaned + val.substring(end);
                        this.setSelectionRange(start + cleaned.length, start + cleaned.length);
                        this.dispatchEvent(new Event('input'));
                    }
                });
            }

            // --- Validation functions ---
            function validateName(value, label) {
                if (!value || value.trim().length === 0) return label + ' é obrigatório.';
                if (!nameRegex.test(value)) return label + ' só pode conter letras, espaços, apóstrofos e hífens.';
                if (value.trim().length < 2) return label + ' deve ter pelo menos 2 caracteres.';
                return null;
            }

            function validateEmail(value) {
                if (!value || value.trim().length === 0) return 'Email é obrigatório.';
                if (/\s/.test(value)) return 'Email não pode conter espaços.';
                if (!emailRegex.test(value)) return 'Formato de e-mail inválido.';
                return null;
            }

            function validateUsername(value) {
                if (!value || value.trim().length === 0) return 'Usuário é obrigatório.';
                if (/\s/.test(value)) return 'O nome de usuário não pode conter espaços.';
                if (!usernameRegex.test(value)) return 'O nome de usuário só pode conter letras, números, ponto (.), hífen (-) e underline (_).';
                if (value.length < 3) return 'O nome de usuário deve ter pelo menos 3 caracteres.';
                return null;
            }

            function validatePassword(value) {
                if (!value || value.length === 0) return 'Senha é obrigatória.';
                if (value.length < 8) return 'A senha deve ter pelo menos 8 caracteres.';
                return null;
            }

            function validatePasswordConfirm(value) {
                if (!value || value.length === 0) return 'Confirmação de senha é obrigatória.';
                if (passwordInput && value !== passwordInput.value) return 'As senhas não coincidem.';
                return null;
            }

            // --- Real-time validation on input and blur ---
            function attachValidation(input, validatorFn) {
                if (!input) return;
                input.addEventListener('input', function() {
                    if (this.value.length > 0) {
                        var error = validatorFn(this.value);
                        if (error) showValidationError(this, error);
                        else removeValidationError(this);
                    } else {
                        removeValidationError(this);
                    }
                });
                input.addEventListener('blur', function() {
                    var error = validatorFn(this.value);
                    if (error) showValidationError(this, error);
                    else removeValidationError(this);
                });
            }

            attachValidation(firstNameInput, function(v) { return validateName(v, 'Nome'); });
            attachValidation(lastNameInput, function(v) { return validateName(v, 'Sobrenome'); });
            attachValidation(emailInput, validateEmail);
            if (usernameInput) attachValidation(usernameInput, validateUsername);
            attachValidation(passwordInput, validatePassword);
            attachValidation(passwordConfirmInput, validatePasswordConfirm);

            // --- Form submit validation ---
            form.addEventListener('submit', function(e) {
                var errors = [];

                var fnErr = validateName(firstNameInput.value, 'Nome');
                if (fnErr) { showValidationError(firstNameInput, fnErr); errors.push(firstNameInput); }
                else removeValidationError(firstNameInput);

                var lnErr = validateName(lastNameInput.value, 'Sobrenome');
                if (lnErr) { showValidationError(lastNameInput, lnErr); errors.push(lastNameInput); }
                else removeValidationError(lastNameInput);

                var emErr = validateEmail(emailInput.value);
                if (emErr) { showValidationError(emailInput, emErr); errors.push(emailInput); }
                else removeValidationError(emailInput);

                if (usernameInput) {
                    var usErr = validateUsername(usernameInput.value);
                    if (usErr) { showValidationError(usernameInput, usErr); errors.push(usernameInput); }
                    else removeValidationError(usernameInput);
                }

                var pwErr = validatePassword(passwordInput.value);
                if (pwErr) { showValidationError(passwordInput, pwErr); errors.push(passwordInput); }
                else removeValidationError(passwordInput);

                var pcErr = validatePasswordConfirm(passwordConfirmInput.value);
                if (pcErr) { showValidationError(passwordConfirmInput, pcErr); errors.push(passwordConfirmInput); }
                else removeValidationError(passwordConfirmInput);

                if (errors.length > 0) {
                    e.preventDefault();
                    errors[0].focus();
                }
            });
        })();
        </script>

        <div class="login-footer">
            <p>Já possui uma conta? <a href="${url.loginUrl}">Entrar</a></p>
        </div>
    </#if>

</@layout.registrationLayout>
