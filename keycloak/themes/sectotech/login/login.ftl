<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=social.displayInfo; section>

    <#if section = "header">
        <h1 class="login-page-title">Entrar</h1>
        <p class="login-subtitle">Acesse sua conta para continuar</p>
    </#if>

    <#if section = "form">

        <#-- Social Providers -->
        <#if realm.password && social.providers??>
            <div class="social-providers">
                <#list social.providers as p>
                    <a href="${p.loginUrl}" class="social-btn" id="social-${p.alias}">
                        <#if p.iconClasses?has_content>
                            <i class="${p.iconClasses!}"></i>
                        </#if>
                        <span>${p.displayName!}</span>
                    </a>
                </#list>
            </div>
            <div class="divider">ou</div>
        </#if>

        <#if realm.password>
            <form id="kc-form-login" action="${url.loginAction}" method="post">

                <div class="form-group">
                    <label for="username">
                        <#if !realm.loginWithEmailAllowed>${msg("username")}
                        <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                        <#else>${msg("email")}</#if>
                    </label>
                    <input
                        tabindex="1"
                        id="username"
                        name="username"
                        value="${(login.username!'')}"
                        type="text"
                        autofocus
                        autocomplete="off"
                        placeholder="seu.usuario ou email@exemplo.com"
                        aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                    />
                    <#if messagesPerField.existsError('username','password')>
                        <span class="alert alert-error" style="margin-top:0.25rem;display:block;">
                            ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                        </span>
                    </#if>
                </div>

                <div class="form-group">
                    <label for="password">${msg("password")}</label>
                    <input
                        tabindex="2"
                        id="password"
                        name="password"
                        type="password"
                        autocomplete="off"
                        placeholder="••••••••"
                        aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                    />
                </div>

                <div class="form-actions">
                    <#if realm.rememberMe && !usernameEditDisabled??>
                        <div class="checkbox-group">
                            <input
                                tabindex="3"
                                id="rememberMe"
                                name="rememberMe"
                                type="checkbox"
                                <#if login.rememberMe??>checked</#if>
                            />
                            <label for="rememberMe">${msg("rememberMe")}</label>
                        </div>
                    <#else>
                        <div></div>
                    </#if>

                    <#if realm.resetPasswordAllowed>
                        <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="forgot-password">
                            ${msg("doForgotPassword")}
                        </a>
                    </#if>
                </div>

                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

                <button tabindex="4" type="submit" class="btn-brand">
                    ${msg("doLogIn")}
                </button>
            </form>

            <script>
            (function() {
                var form = document.getElementById('kc-form-login');
                var usernameInput = document.getElementById('username');
                var passwordInput = document.getElementById('password');

                // Keycloak default: letters, numbers, dot, hyphen, underscore
                /*
                var usernameRegex = /^[a-zA-Z0-9._\-]+$/;
                */

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

                /*
                function validateUsername(value) {
                    if (!value || value.trim().length === 0) {
                        return 'O campo de usuário é obrigatório.';
                    }
                    if (/\s/.test(value)) {
                        return 'O nome de usuário não pode conter espaços.';
                    }
                    if (!usernameRegex.test(value)) {
                        return 'O nome de usuário só pode conter letras, números, ponto (.), hífen (-) e underline (_).';
                    }
                    if (value.length < 3) {
                        return 'O nome de usuário deve ter pelo menos 3 caracteres.';
                    }
                    return null;
                }
                */

                function validatePassword(value) {
                    if (!value || value.length === 0) {
                        return 'O campo de senha é obrigatório.';
                    }
                    return null;
                }

                // Real-time validation on input
                /*
                usernameInput.addEventListener('input', function() {
                    var value = this.value;
                    if (value.length > 0) {
                        var error = validateUsername(value);
                        if (error) {
                            showValidationError(this, error);
                        } else {
                            removeValidationError(this);
                        }
                    } else {
                        removeValidationError(this);
                    }
                });

                // Validate on blur
                usernameInput.addEventListener('blur', function() {
                    var error = validateUsername(this.value);
                    if (error) {
                        showValidationError(this, error);
                    } else {
                        removeValidationError(this);
                    }
                });
                */

                passwordInput.addEventListener('blur', function() {
                    var error = validatePassword(this.value);
                    if (error) {
                        showValidationError(this, error);
                    } else {
                        removeValidationError(this);
                    }
                });

                // Form submit validation
                form.addEventListener('submit', function(e) {
                    // var usernameError = validateUsername(usernameInput.value);
                    var passwordError = validatePassword(passwordInput.value);
                    var hasError = false;

                    /*
                    if (usernameError) {
                        showValidationError(usernameInput, usernameError);
                        hasError = true;
                    } else {
                        removeValidationError(usernameInput);
                    }
                    */

                    if (passwordError) {
                        showValidationError(passwordInput, passwordError);
                        hasError = true;
                    } else {
                        removeValidationError(passwordInput);
                    }

                    if (hasError) {
                        e.preventDefault();
                        passwordInput.focus();
                    }
                });
            })();
            </script>
        </#if>

        <#-- Link para registro -->
        <div class="login-footer">
            <p>Não possui uma conta ainda? <a href="${properties.frontendBaseUrl}/registrar">Registre-se</a></p>
        </div>

    </#if>

</@layout.registrationLayout>
