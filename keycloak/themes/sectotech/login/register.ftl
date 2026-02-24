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

        <div class="login-footer">
            <p>Já possui uma conta? <a href="${url.loginUrl}">Entrar</a></p>
        </div>
    </#if>

</@layout.registrationLayout>
