<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp','userLabel'); section>

    <#if section = "header">
        <h1 class="login-page-title">Configurar Autenticação</h1>
        <p class="login-subtitle">Configure a autenticação de dois fatores</p>
    </#if>

    <#if section = "form">
        <form id="kc-otp-login-form" action="${url.loginAction}" method="post">

            <#if otpLogin.userOtpCredentials?size gt 1>
                <div class="form-group">
                    <#list otpLogin.userOtpCredentials as otpCredential>
                        <div class="checkbox-group" style="margin-bottom:0.5rem;">
                            <input
                                type="radio"
                                id="kc-otp-credential-${otpCredential?index}"
                                name="selectedCredentialId"
                                value="${otpCredential.id}"
                                <#if otpCredential.id == otpLogin.selectedCredentialId>checked="checked"</#if>
                            />
                            <label for="kc-otp-credential-${otpCredential?index}">
                                ${otpCredential.userLabel}
                            </label>
                        </div>
                    </#list>
                </div>
            </#if>

            <div class="form-group">
                <label for="otp">Código de Verificação</label>
                <input
                    type="text"
                    id="otp"
                    name="otp"
                    autocomplete="off"
                    autofocus
                    placeholder="000000"
                    aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"
                />
                <#if messagesPerField.existsError('totp')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('totp'))?no_esc}
                    </span>
                </#if>
            </div>

            <button type="submit" class="btn-brand">Verificar</button>
        </form>

        <script>
        (function() {
            var form = document.getElementById('kc-otp-login-form');
            var otpInput = document.getElementById('otp');

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

            // Block non-digit characters on keypress
            otpInput.addEventListener('keypress', function(e) {
                var char = String.fromCharCode(e.which || e.keyCode);
                if (e.ctrlKey || e.metaKey || e.which < 32) return;
                if (!/[0-9]/.test(char)) {
                    e.preventDefault();
                }
            });

            // Sanitize paste - only digits
            otpInput.addEventListener('paste', function(e) {
                var pasted = (e.clipboardData || window.clipboardData).getData('text');
                var cleaned = pasted.replace(/[^0-9]/g, '');
                if (cleaned !== pasted) {
                    e.preventDefault();
                    var start = this.selectionStart;
                    var end = this.selectionEnd;
                    var val = this.value;
                    this.value = val.substring(0, start) + cleaned + val.substring(end);
                    this.setSelectionRange(start + cleaned.length, start + cleaned.length);
                }
            });

            // Real-time validation
            otpInput.addEventListener('input', function() {
                // Remove any non-digit that got through
                this.value = this.value.replace(/[^0-9]/g, '');
                if (this.value.length > 0) {
                    removeValidationError(this);
                }
            });

            // Form submit validation
            form.addEventListener('submit', function(e) {
                if (!otpInput.value || otpInput.value.trim().length === 0) {
                    e.preventDefault();
                    showValidationError(otpInput, 'O código de verificação é obrigatório.');
                    otpInput.focus();
                } else if (!/^\d+$/.test(otpInput.value)) {
                    e.preventDefault();
                    showValidationError(otpInput, 'O código deve conter apenas números.');
                    otpInput.focus();
                } else {
                    removeValidationError(otpInput);
                }
            });
        })();
        </script>
    </#if>

</@layout.registrationLayout>
