<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true displayWide=false>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>

    <title>${msg("loginTitle",(realm.displayName!'SECTOTECH'))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" type="image/x-icon" />

    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>

    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>

    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>

<body class="${bodyClass}">
    <div class="login-container">
        <div class="login-card">

            <!-- Logo -->
            <div class="login-header">
                <img src="${url.resourcesPath}/img/ocrsolution-logo.png" alt="OCR Solution" class="login-logo" />
            </div>

            <!-- Título da página -->
            <div class="login-title-section">
                <#nested "header">
            </div>

            <!-- Mensagens de feedback -->
            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="alert alert-${message.type}">
                    <span class="alert-text">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <!-- Conteúdo do formulário -->
            <div class="login-form-wrapper">
                <#nested "form">
            </div>

            <!-- Informações adicionais -->
            <#if displayInfo>
                <div class="login-info">
                    <#nested "info">
                </div>
            </#if>

        </div>
    </div>
</body>
</html>
</#macro>
