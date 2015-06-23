<#import "base.ftl" as base/>
<@base.page>
<div class="row">
    <div class="large-12 columns">
        <h3>Your Microservice Foundation</h3>
        <hr/>
    </div>
</div>
<div class="row">
    <div class="large-4 columns">
        <h4>Sample Data</h4>
        <table width="100%">
            <thead>
                <tr><th>ID</th><th>Item</th></tr>
            </thead>
            <tbody>
                <#list items as item>
                    <tr><td>${item.id}</td><td>${item.name}</td></tr>
                </#list>
            </tbody>
        </table>
    </div>
    <div class="large-8 columns">
        <h4>Bootstrapped</h4>
        <p>This quickstart demonstrates several capabilities:</p>
        <ul>
            <li>Example Freemarker templates with inheritance, WebJars & localization</li>
            <li>Example JCache configuration backed by Infinispan</li>
            <li>Example Pippo Routes and Fathom RESTful Controllers</li>
            <li>Example Form authentication backed by a Memory Realm</li>
            <li>Automatic (de)serialization to/from xml & json using JAXB & GSON</li>
            <li>Automatic Swagger specification generation</li>
            <li>Builds Stork & Capsule distributables</li>
        </ul>

        <h4>Documented API</h4>
        Your generated Swagger API specification is <a target="_blank" href="${appPath}/api/swagger.json">here</a>.<br/>
        You can explore it with the integrated Swagger UI <a target="_blank" href="${appPath}/api">here</a>.<br/>
        And you can even use <a target="_blank" href="http://swagger.io/tools">Swagger.io tools</a> to generate clients from this specification!
    </div>
</div>
<footer>
    <div class="row">
        <hr/>
        <b>${i18n('fathom.dispatchingSince')}</b> ${formatTime(bootDate, "full")}
        (<i>${prettyTime(bootDate)}</i>)
    </div>
</footer>

</@base.page>