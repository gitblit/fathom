<#-------------------------------------------------------
    base.ftl

    This Freemarker template demonstrates:
      1. webjarsAt function
      2. publicAt function
      3. condition statements
      4. localized messages
      5. child block declaration

    Freemarker documentation
    http://freemarker.org

-------------------------------------------------------->
<#macro page>
<!DOCTYPE html>
<!--[if IE 9]><html class="lt-ie10" lang="en" > <![endif]-->
<html class="no-js" lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${appName}</title>
    <link rel="stylesheet" href="${webjarsAt("normalize.css/normalize.css")}">
    <link rel="stylesheet" href="${webjarsAt("foundation/css/foundation.min.css")}">
    <link rel="stylesheet" href="${webjarsAt("font-awesome/css/font-awesome.min.css")}">
    <link rel="stylesheet" href="${publicAt("css/custom.css")}">
</head>
<body>
<div class="row">
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
</div>

<div style="padding-top:10px;">
    <#nested/>
</div>

<script src="${webjarsAt("modernizr/modernizr.min.js")}"></script>
<script src="${webjarsAt("jquery/jquery.min.js")}"></script>
<script src="${webjarsAt("foundation/js/foundation.min.js")}"></script>
<script>
    $(document).foundation();
</script>
</body>
</html>
</#macro>