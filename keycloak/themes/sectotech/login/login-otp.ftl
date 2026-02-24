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
    </#if>

</@layout.registrationLayout>
