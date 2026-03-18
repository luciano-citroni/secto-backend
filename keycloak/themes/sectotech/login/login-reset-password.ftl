<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>

    <#if section = "header">
        <h1 class="login-page-title">Esqueceu a senha?</h1>
        <p class="login-subtitle">Informe seu usuário ou email para redefinir sua senha</p>
    </#if>

    <#if section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">

            <div class="form-group">
                <label for="username">
                    <#if !realm.loginWithEmailAllowed>${msg("username")}
                    <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                    <#else>${msg("email")}</#if>
                </label>
                <input
                    type="text"
                    id="username"
                    name="username"
                    autofocus
                    autocomplete="off"
                    placeholder="seu.usuario ou email@exemplo.com"
                    aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                />
                <#if messagesPerField.existsError('username')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}
                    </span>
                </#if>
            </div>

            <button type="submit" class="btn-brand">Enviar</button>
        </form>

        <script>
        (function() {
            var form = document.getElementById('kc-reset-password-form');
            var usernameInput = document.getElementById('username');
            var usernameRegex = /^[a-zA-Z0-9._\-]+$/;

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

            function validateUsername(value) {
                if (!value || value.trim().length === 0) return 'O campo é obrigatório.';
                if (/\s/.test(value)) return 'Não pode conter espaços.';
                if (!usernameRegex.test(value)) return 'Só pode conter letras, números, ponto (.), hífen (-) e underline (_).';
                if (value.length < 3) return 'Deve ter pelo menos 3 caracteres.';
                return null;
            }

            // Real-time validation
            usernameInput.addEventListener('input', function() {
                if (this.value.length > 0) {
                    var error = validateUsername(this.value);
                    if (error) showValidationError(this, error);
                    else removeValidationError(this);
                } else {
                    removeValidationError(this);
                }
            });

            usernameInput.addEventListener('blur', function() {
                var error = validateUsername(this.value);
                if (error) showValidationError(this, error);
                else removeValidationError(this);
            });

            // Form submit validation
            form.addEventListener('submit', function(e) {
                var error = validateUsername(usernameInput.value);
                if (error) {
                    e.preventDefault();
                    showValidationError(usernameInput, error);
                    usernameInput.focus();
                } else {
                    removeValidationError(usernameInput);
                }
            });
        })();
        </script>

        <a href="${url.loginUrl}" class="back-link">&larr; Voltar ao login</a>
    </#if>

    <#if section = "info">
        <p>${msg("emailInstruction")}</p>
    </#if>

</@layout.registrationLayout>
