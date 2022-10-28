<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${dictionary.name}">
	<jsp:body>
	
		<style>
		
			#item-list {
				margin-top: 2rem;
			}
			
			#item-list input[type=text] {
				width: 100%;
			}
		
		</style>
	
		<script>
		
			$(document).ready(function() {
				
				renderItems($("#item-list"));
				
			});
			
			function getItemRow(item)
			{
				var row = $("<tr></tr>").attr("id", "item-" + item.id);
				
				row.append($("<td></td>").append($("<input></input>").attr("type", "text").attr("name", "name").addClass("km-input").val(item.name)));
				row.append($("<td></td>").append($("<input></input>").attr("type", "text").attr("name", "key").addClass("km-input").val(item.key)));
				
				var btnCell = $("<td></td>");
				
				showDeleteBtn(row, btnCell, item.id);
				
				row.append(btnCell);
				
				row.find("input[name=name]").focusout((function(itemId) {
					
					return function() {
						
						var record = {
							id: itemId,
							name: $(this).val()
						};
						
						var onSuccess = function() {
							$.post(km.js.config.contextPath + "/km/dictionaries/initonenv");
							//$(this).css("background", "green");
						}
						
						km.js.db.update(record, onSuccess, function() {
							km.js.ui.statusbar.err("Saving item failed");
						});
						
					}
					
				})(item.id));
				
				row.find("input[name=key]").focusout((function(itemId) {
					
					return function() {
						
						var record = {
							id: itemId,
							key: $(this).val()
						};
						
						var onSuccess = function() {
							$.post(km.js.config.contextPath + "/km/dictionaries/initonenv");
							//$(this).css("background", "green");
						}
						
						km.js.db.update(record, onSuccess, function() {
							km.js.ui.statusbar.err("Saving item failed");
						});
						
					}
					
				})(item.id));
				
				return row;
			}
			
			function showDeleteBtn (itemRow, target, itemId)
			{	
				var check = $("<img></img>").attr("src", km.js.config.imagePath + "/trashicon.png").addClass("task-icon");
				
				target.append(check);
				
				// ask user to confirm before removing
				km.js.ui.confirm({
					target: check,
					callback: (function(itemId, itemRow) {
						
						return function() {
							$.post(km.js.config.contextPath + "/km/dictionaries/deleteitem", { id: itemId }, (function(itemRow) {
								
								return function(resp) {
									if (resp.success)
									{
										itemRow.fadeOut(400);
									}
									else
									{
										km.js.ui.statusbar.err("Error deleting item", 5000);
									}
								}
								
							})(itemRow), "json");
						}
						
					})(itemId, itemRow) 
				})
				
			}
			
			function renderItems(target)
			{
				$.get(km.js.config.contextPath + "/km/dictionaries/items/${dictionary.id}", (function(target) {
					

					return function(resp) {
						
						var table = $("<table></table>").addClass("std-table item-table").attr("id", "items-table");
						
						var row = $("<tr></tr>").addClass("cols");
						row.append($("<th></th>").text("Name"));
						row.append($("<th></th>").text("Key"));
						row.append($("<th></th>"));
						
						table.append($("<thead></thead>").append(row));
						
						var tbody = $("<tbody></tbody>");
						
						var editRow = $("<tr></tr>").attr("id", "new-item");
						editRow.append($("<td></td>").append($("<input></input>").attr("type", "text").attr("name", "name").addClass("km-input")));
						editRow.append($("<td></td>").append($("<input></input>").attr("type", "text").attr("name", "key").addClass("km-input")));
						
						var saveItemBtn = $("<input></input>").attr("type", "button").val("Save").addClass("sbtn").attr("id", "saveItemBtn");
						
						saveItemBtn.click(function() {
							
							var item = {
								dictionaryId: "${dictionary.id}",
								name: $("#new-item input[name='name']").val(),
								key: $("#new-item input[name='key']").val(),
								index: 0
							};
							
							$.post(km.js.config.contextPath + "/km/dictionaries/saveitem", item, (function(item) {
								
								return function(resp) {
									if (resp.success)
									{
										item.id = resp.data.itemId;
										$("table#items-table > tbody").append(getItemRow(item));
										$("#new-item input[name='name']").val("");
										$("#new-item input[name='key']").val("");
									}
								}
								
							})(item), "json");
							
						});
						
						editRow.append($("<td></td>").append(saveItemBtn));
						tbody.append(editRow);
						
						for (var i = 0; i < resp.data.items.length; i++)
						{
							var item = resp.data.items[i];
							var row = getItemRow(item);
							tbody.append(row);
						}
						
						table.append(tbody);
						target.empty().append(table);
						
					};
					
				})(target), "json");
			}
			
			function deleteDictionary()
			{
				$.post("${pageContext.request.contextPath}/km/dictionaries/delete", { id: "${dictionary.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/dictionaries/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<input type="hidden" name="dictionaryId" value="${dictionary.id}" />
			
			<ko:pageHeader>${dictionary.name}</ko:pageHeader>
			
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/dictionaries/edit/${dictionary.id}" class="sbtn">Edit</a>						
				<a href="javascript:;" onclick="ask('Are you sure you want to delete this dictionary?', 'warnPrompt', function() { deleteApp(); })" id="deleteDictionaryBtn" class="sbtn">Delete</a>
			</ko:buttonPanel>
				
			<div id="warnPrompt" style="margin-top:10px"></div>
			
			<ko:propertyTable>
				<ko:propertyRow>
					<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
					<ko:propertyValue>${dictionary.name}</ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
			
			<div id="item-list"></div>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>