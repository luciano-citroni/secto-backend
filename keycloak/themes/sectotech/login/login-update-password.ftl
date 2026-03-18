<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>

    <#if section = "header">
        <h1 class="login-page-title">Atualizar Senha</h1>
        <p class="login-subtitle">Defina uma nova senha para sua conta</p>
    </#if>

    <#if section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">

            <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly="readonly" style="display:none;"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>

            <div class="form-group">
                <label for="password-new">Nova Senha</label>
                <input
                    type="password"
                    id="password-new"
                    name="password-new"
                    autofocus
                    autocomplete="new-password"
                    placeholder="••••••••"
                    aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                />
                <#if messagesPerField.existsError('password')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="form-group">
                <label for="password-confirm">Confirmar Nova Senha</label>
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

            <div class="form-actions" style="justify-content: flex-end;">
                <#if isAppInitiatedAction??>
                    <button type="submit" class="btn-brand" style="width:auto;flex:1;">Salvar</button>
                    <button type="submit" class="btn-link" name="cancel-aia" value="true" style="width:auto;">Cancelar</button>
                <#else>
                    <button type="submit" class="btn-brand">Salvar</button>
                </#if>
            </div>
        </form>

        <script>
        (function() {
            var form = document.getElementById('kc-passwd-update-form');
            var passwordNewInput = document.getElementById('password-new');
            var passwordConfirmInput = document.getElementById('password-confirm');

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

            function validatePassword(value) {
                if (!value || value.length === 0) return 'A nova senha é obrigatória.';
                if (value.length < 8) return 'A senha deve ter pelo menos 8 caracteres.';
                return null;
            }

            function validatePasswordConfirm(value) {
                if (!value || value.length === 0) return 'A confirmação de senha é obrigatória.';
                if (passwordNewInput && value !== passwordNewInput.value) return 'As senhas não coincidem.';
                return null;
            }

            // Real-time validation
            passwordNewInput.addEventListener('input', function() {
                if (this.value.length > 0) {
                    var error = validatePassword(this.value);
                    if (error) showValidationError(this, error);
                    else removeValidationError(this);
                } else {
                    removeValidationError(this);
                }
                // Revalidate confirm if it has a value
                if (passwordConfirmInput.value.length > 0) {
                    var cErr = validatePasswordConfirm(passwordConfirmInput.value);
                    if (cErr) showValidationError(passwordConfirmInput, cErr);
                    else removeValidationError(passwordConfirmInput);
                }
            });

            passwordConfirmInput.addEventListener('input', function() {
                if (this.value.length > 0) {
                    var error = validatePasswordConfirm(this.value);
                    if (error) showValidationError(this, error);
                    else removeValidationError(this);
                } else {
                    removeValidationError(this);
                }
            });

            passwordNewInput.addEventListener('blur', function() {
                var error = validatePassword(this.value);
                if (error) showValidationError(this, error);
                else removeValidationError(this);
            });

            passwordConfirmInput.addEventListener('blur', function() {
                var error = validatePasswordConfirm(this.value);
                if (error) showValidationError(this, error);
                else removeValidationError(this);
            });

            // Form submit validation
            form.addEventListener('submit', function(e) {
                var errors = [];

                var pwErr = validatePassword(passwordNewInput.value);
                if (pwErr) { showValidationError(passwordNewInput, pwErr); errors.push(passwordNewInput); }
                else removeValidationError(passwordNewInput);

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
    </#if>

</@layout.registrationLayout>
