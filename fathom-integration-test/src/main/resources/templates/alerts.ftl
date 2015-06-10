<#--
 * Prints a single alert message.
 *
 * @param message
 * @param type
 -->
<#macro single message type="info">
    <#if message??>
    <div data-alert class="alert-box ${type} round">
    ${message}
        <a href="#" class="close">&times;</a>
    </div>
    </#if>
</#macro>

<#--
 * Prints a list of alert messages.
 *
 * @param messages
 * @param type
 -->
<#macro list messages type="info">
    <#if messages?has_content>
    <div class="message-list">
        <#list messages as message>
            <div data-alert class="alert-box ${type} round">
            ${message}
                <a href="#" class="close">&times;</a>
            </div>
        </#list>
    </div>
    </#if>
</#macro>
