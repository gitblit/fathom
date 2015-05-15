<#macro page>
<!DOCTYPE html>
<!--[if IE 9]><html class="lt-ie10" lang="en" > <![endif]-->
<html class="no-js" lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${appName}</title>
    <link rel="stylesheet" href="${webjarsAt("normalize.css/3.0.1/normalize.css")}">
    <link rel="stylesheet" href="${webjarsAt("foundation/5.5.2/css/foundation.min.css")}">
    <link rel="stylesheet" href="${webjarsAt("font-awesome/4.3.0/css/font-awesome.min.css")}">
    <link rel="stylesheet" href="${publicAt("css/custom.css")}">
</head>
<body>
<nav class="top-bar" data-topbar role="navigation">
    <ul class="title-area">
        <li class="name">
            <h1><a href="${appPath}/"><i class="fa fa-anchor"></i> ${appName}</a></h1>
        </li>
    </ul>

    <section class="top-bar-section">
        <!-- Right Nav Section -->
        <ul class="right">
            <li><a>${appVersion}</a></li>
            <li class="divider"></li>
            <#if account??>
                <li class="has-dropdown hover">
                    <a href="#">${account}</a>
                    <ul class="dropdown">
                        <li><label>${i18n('fathom.userActions')}</label></li>
                        <li><a href="${contextPath}/logout"><i class="fa fa-sign-out"></i> Logout</a></li>
                    </ul>
                </li>
            <#else>
                <li class="has-form">
                    <a class="button" href="${contextPath}/login"><i class="fa fa-sign-in"></i> Login</a>
                </li>
            </#if>
        </ul>

        <!-- Left Nav Section -->
        <ul class="left">
        </ul>
    </section>
</nav>

<div style="padding-top:10px;">
    <#nested/>
</div>

<script src="${webjarsAt("modernizr/2.8.3/modernizr.min.js")}"></script>
<script src="${webjarsAt("jquery/2.1.1/jquery.min.js")}"></script>
<script src="${webjarsAt("foundation/5.5.2/js/foundation.min.js")}"></script>
<script>
    $(document).foundation();
</script>
</body>
</html>
</#macro>