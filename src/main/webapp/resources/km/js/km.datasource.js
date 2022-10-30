/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.datasource = {

	create: function(settings) {
	
		var defaultSettings = {
			contextPath: km.js.config.contextPath,
			sysContextPath: km.js.config.sysContextPath
		}
		
		var options = $.extend({}, defaultSettings, settings);
		
		// create datasource object
		var ds = {
			type: options.type,
			data: options.data,
			jsti: options.jsti,
			jcr: null,
			contextPath: options.contextPath,
			sysContextPath: options.sysContextPath,
			
			// the length of the original data collection, before any filtering or pagination is applied
			dataLength: km.js.utils.isEmpty(options.data) ? null : options.data.length,
			
			// the length of the data collection after filtering has been applied, but without pagination
			//filteredDataLength: km.js.utils.isEmpty(options.data) ? null : options.data.length,
			
			// default compare function
			getDefaultCompare: function (property, isDescending) {
			
				return function(o1, o2) {
					var val1 = km.js.utils.propVal(o1, property);
					var val2 = km.js.utils.propVal(o2, property);
					
					if (val1 < val2)
					{
						return isDescending ? 1 : -1;
					}
					if (val1 > val2)
					{
						return isDescending ? -1 : 1;
					}
					return 0;
				}
			},
			
			query: function(jcrOrQuery, callback, onFailure) {
				
				var jcr = null;
				var query = null;
				
				if (jcrOrQuery && typeof(jcrOrQuery) === 'object')
				{
					jcr = jcrOrQuery;
				}
				else
				{
					query = jcrOrQuery;
				}
				
				if (this.type === "collection")
				{
					// with offline collections, JSTI is known before the query is made, so we can
					// populate JCR property IDs before querying. Additionally, this is required because
					// query filtering functions use property IDs, not names.
					if (jcr)
					{
						this.populateJCRIds(jcr, this.jsti);
					}
					return this.queryLocalCollection(jcr, callback);
				}
				else if (this.type === "database")
				{
					// with online datasource, the JSTI is not known before issuing the query, so we
					// need to wait with populating property IDs.
					var callbackCaller = function(callback, ds) {
						
						return function(records, recordCount, jsti) {
							if (jcr)
							{
								ds.populateJCRIds(jcr, jsti);
							}
							callback(records, recordCount, jsti)
						}
						
					};
					
					// query either by JCR or string query
					return this.queryDatabase(jcr ? jcr : query, callbackCaller(callback, this), onFailure);
				}
				else if (this.type === "json")
				{
					// this is a special case where the datasource is a javascript array of json objects
					// and the jcr is in fact not a regular jcr
					// the different with this JCR is that it has property names, not IDs, under "property_id" value in restrictions
					return this.queryJSONCollection(jcr, callback);
				}
				else
				{
					console.error("Unsupported data source type " + this.type);
				}
			},
			
			/**
			 * Queries a datasource of type "database" and, on success, calls the callback method
			 * passing the returned JSRC as parameter.
			 */
			queryDatabase: function(jcrOrQuery, callback, onFailure) {
				
				var jcr = null;
				var query = null;
				var jsdsParams = null;
				
				if (typeof(jcrOrQuery) === 'object')
				{
					jcr = jcrOrQuery;
					this.jcr = jcr;
					
					jsdsParams = { jcr: JSON.stringify(jcr), mode: "datasource" };
				}
				else
				{
					query = jcrOrQuery;
					this.jcr = null;
					
					jsdsParams = { query: query, mode: "datasource" };
				}
				
				if (typeof(this.contextPath) === "undefined")
				{
					throw "Context path not set";
				}
				
				var queryDatabaseCallback = function (dataSource, userCallback, onFailure) {
					
					return function(data) {
						
						// data returned by the /rest/jsds/query action with mode = "datasource" switch
						// will return an object that looks like:
						// { jsrc: { ... }, recordCount: ... }
						
						dataSource.jsti = data.jsrc.jsti;
						dataSource.dataLength = data.recordCount;
						
						// invoke callback function defined by user
						if (typeof(userCallback) === "function")
						{
							userCallback(data.jsrc.records, data.recordCount, data.jsrc.jsti);
						}
					}
					
				};
				
				$.post(this.sysContextPath + "/rest/jsds/query", jsdsParams, queryDatabaseCallback(this, callback), "json").fail(onFailure);
			},
			
			/**
			 * Queries a datasource of type "collection" and, on success, calls the callback method
			 * passing the returned JSRC as parameter.
			 */
			queryJSONCollection: function(jcr, callback) {
				this.jcr = jcr;
				this.dataLength = this.data.length;
				var filteredData = this.filterData();
				
				// TODO add support for groupings here coping it from function queryLocalCollection()
				// and adding modifications
				
				// set length of the filtered data collection, after grouping has been applied
				// but before pagination
				var unpagedDataLength = filteredData.length;
				
				// sort data - this has to be done after all the groupings are applied
				filteredData = this.sortItems(filteredData);
				
				filteredData = this.pageItems(filteredData, this.jcr.limit, this.jcr.offset);
				
				if (typeof(callback) === "function")
				{
					// pass to the callback the filtered, paged and sorted collection
					// as well as the number of items before pagination
					callback(filteredData, unpagedDataLength, this.jsti);
				}
				
				return filteredData;
			},
			
			/**
			 * Queries a datasource of type "collection" and, on success, calls the callback method
			 * passing the returned JSRC as parameter.
			 */
			queryLocalCollection: function(jcr, callback) {
			
				this.jcr = jcr;
				this.dataLength = this.data.length;
				var filteredData = this.filterData();
				
				// the list of values by which records are grouped
				// we need to keep them in a list although we also have them as keys
				// in the groupedData object, because using them as keys in an object
				// converts them to string so we would lose information about their data type,
				// which we need to perform calculations/sorting
				var groupValues = [];
				
				// apply groupings
				if (jcr.groupings != null && jcr.groupings.length > 0)
				{
					var groupedData = {};
					
					// assume there is only one grouping
					var groupPropId = jcr.groupings[0].property_id;
					
					for (var i = 0; i < filteredData.length; i++)
					{
						var rec = filteredData[i];
						var val = km.js.utils.propVal(rec, groupPropId);
						
						if (km.js.utils.isEmpty(val))
						{
							val = "";
						}
						
						if ($.inArray(val, groupValues) < 0)
						{
							groupValues.push(val);
						}
						
						var groupRow = null;
						
						// check if this group already exists
						if (!km.js.utils.isEmpty(groupedData[val]))
						{
							groupRow = groupedData[val];
						}
						else
						{
							groupRow = { group_value: val, items: [], aggr_results: {} };
						}
						
						groupRow.items.push(rec);
						
						groupedData[val] = groupRow;
					}
					
					var groupedItems = [];
					
					// for each displayed property, get the aggregate value
					for (var k = 0; k < groupValues.length; k++)
					{
						var groupVal = groupValues[k];
						
						// cast to string while getting property from object
						var group = groupedData[groupVal];
						
						km.js.utils.setPropVal(group, groupPropId, groupVal);
						
						for (var i = 0; i < jcr.properties.length; i++)
						{
							if (jcr.properties[i].id != groupPropId)
							{
								group[jcr.properties[i].id] = group.items.length;
							}
						}
						
						// rewrite object to array
						groupedItems.push(group);
					}
					
					// set aggregate function values within groups
					for (var i = 0; i < groupedItems.length; i++)
					{
						var group = groupedItems[i];
						
						// iterate over all records within this group to collect values for aggregate functions
						for (var j = 0; j < group.items.length; j++)
						{
							var rec = group.items[j];
							
							for (var k = 0; k < jcr.properties.length; k++)
							{
								var prop = jcr.properties[k];
								
								// check if aggregate statistics exist for this property in this group
								if (km.js.utils.isEmpty(group.aggr_results[prop.id]))
								{
									group.aggr_results[prop.id] = { sum: 0, count: 0, min: null, max: null, avg: null };
								}
								
								var aggr_results = group.aggr_results[prop.id]; 
								
								if (prop.id != groupPropId)
								{
									var aggr = prop.aggr;
									var propId = prop.id;
									
									if (aggr === "avg")
									{
										var val = km.js.utils.propVal(rec, propId);
										aggr_results.sum += (val != null ? val : 0);				
										aggr_results.count = group.items.length;
									}
									// value sum will be needed for both sum and avg aggregate functions
									else if (aggr === "sum")
									{
										var val = km.js.utils.propVal(rec, propId);
										aggr_results.sum += (val != null ? val : 0);
									}
									else if (aggr === "count")
									{
										aggr_results.count = group.items.length;
									}
									else if (aggr === "min")
									{
										var val = km.js.utils.propVal(rec, propId);
										if (aggr_results.min == null || val < aggr_results.min)
										{
											aggr_results.min = val;
										}
									}
									else if (aggr === "max")
									{
										var val = km.js.utils.propVal(rec, propId);
										if (aggr_results.max == null || val > aggr_results.max)
										{
											aggr_results.max = val;
										}
									}
								}
								
								aggr_results.avg = (aggr_results.sum / aggr_results.count).toFixed(2);
							}
						}
						
						for (var k = 0; k < jcr.properties.length; k++)
						{
							var aggr = jcr.properties[k].aggr;
							if (!km.js.utils.isEmpty(aggr))
							{
								group[jcr.properties[k].id] = group.aggr_results[jcr.properties[k].id][aggr];
							}
						}
					}
					
					filteredData = groupedItems;
				}
				
				// set length of the filtered data collection, after grouping has been applied
				// but before pagination
				var unpagedDataLength = filteredData.length;
				
				// sort data - this has to be done after all the groupings are applied
				filteredData = this.sortItems(filteredData);
				
				filteredData = this.pageItems(filteredData, this.jcr.limit, this.jcr.offset);
				
				if (typeof(callback) === "function")
				{
					// pass to the callback the filtered, paged and sorted collection
					// as well as the number of items before pagination
					callback(filteredData, unpagedDataLength, this.jsti);
				}
				
				return filteredData;
			},
			
			filterData: function() {
			
				var filteredData = [];
				
				// if no jcr is defined, just return the whole data set
				if (km.js.utils.isEmpty(this.jcr))
				{
					return [].concat(this.data);
				}
				
				// filter by restrictions
				if (this.jcr.restrictions != null && this.jcr.restrictions.length > 0)
				{
					for (var k = 0; k < this.data.length; k++)
					{
						var rec = this.data[k];
						
						var meetsCriteria = true;
						for (var i = 0; i < this.jcr.restrictions.length && meetsCriteria; i++)
						{
							var restr = this.jcr.restrictions[i];
							//console.log("Restr: " + JSON.stringify(restr) + " == " + this.isMeetCriteria(restr, rec));
							if (!this.isMeetCriteria(restr, rec))
							{
								meetsCriteria = false;
							}
						}
						
						if (meetsCriteria)
						{
							filteredData.push(rec);
						}
					}
				}
				else
				{
					// copy all data into a new array
					filteredData = [].concat(this.data);
				}
				
				return filteredData;
			},

			// apply pagination to the items collection
			pageItems: function(items, limit, offset) {
				var actualOffset = km.js.utils.isEmpty(offset) ? 0 : offset;
				var actualLimit = km.js.utils.isEmpty(limit) ? (items.length - actualOffset) : limit;
				return items.splice(actualOffset, actualLimit);
			},
			
			sortItems: function(items) {
				
				if (km.js.utils.isEmpty(this.jcr.orderings) || this.jcr.orderings.length == 0)
				{
					return items;
				}
				
				sortProperty = this.jcr.orderings[0].property_id;
				sortOrder = this.jcr.orderings[0].direction;
				
				return items.sort(this.getDefaultCompare(sortProperty, sortOrder === "desc"));
			},
			
			/**
			 * This method takes a JCR and if this JCR contains properties with only the name of a property
			 * defined (in a SELECT property, ordering, grouping or restriction), it populates also the PIR
			 * of that property basing on the JSTI.
			 * @private
			 */
			populateJCRIds: function (jcr, jsti) {
				
				// get type name basing on type ID
				var type = km.js.utils.getTypeFromJSTI(jcr, jsti);
				var typeName = type.qualifiedName;
				
				if (!typeName)
				{
					throw "Type qualified name not found in JSTI. Type ID is " + jcr.baseTypeId;
				}
				
				/*var getPirFromJsti = function (typeName, propName, jsti) {
					if (propName.indexOf(".") === -1) {
						return [ jsti.pirs[typeName + "." + propName] ];
					}
					else {
						var firstPropName = propName.substring(0, propName.indexOf("."));
						var nextPropNames = propName.substring(propName.indexOf(".") + 1);
						
						var firstProp = jsti.pirs[typeName + "." + firstPropName];
						if (!firstProp)
						{
							throw "Property PIR " + firstPropName + " (part of " + propName + ") not found on type " + typeName;
						}
						
						var field = jsti.fields[firstProp];
						if (!field)
						{
							throw "Field " + firstProp + " (part of " + propName + ") not found on type " + typeName + " (although property PIR is found)";
						}
						
						var nestedType = null;
						
						if (field.dataType.id === km.js.datatypes.object_reference.id)
						{
							nestedType = jsti.types[field.dataType.typeId];
							if (!nestedType)
							{
								throw "Type with ID " + field.dataType.typeId + " not found in JSTI";
							}
							
							//console.log("NE " + JSON.stringify(nestedType));
						}
						else
						{
							throw "Field " + firstPropName + " is not an object reference, but is used with nested properties";
						}
						
						return [ firstProp ].concat(getPirFromJsti(nestedType.qualifiedName, nextPropNames, jsti));
					}
				};*/
				
				// populate PIRs on SELECT properties
				if (jcr.properties)
				{
					for (var i = 0; i < jcr.properties.length; i++)
					{
						var prop = jcr.properties[i];
						if (!prop.id)
						{
							prop.id = km.js.utils.nestedPropertyToPir(prop.name, type, jsti);
						}
					}
				}
				
				// populate PIRs on groupings
				if (jcr.groupings)
				{
					for (var i = 0; i < jcr.groupings.length; i++)
					{
						var group = jcr.grouping[i];
						if (!group.property_id)
						{
							group.property_id = nestedPropertyToPir(group.property_name, type, jsti);
						}
					}
				}
				
				// populate PIRs on orderings
				if (jcr.orderings)
				{
					for (var i = 0; i < jcr.orderings.length; i++)
					{
						var ordering = jcr.orderings[i];
						if (!ordering.property_id)
						{
							ordering.property_id = nestedPropertyToPir(ordering.property_name, type, jsti);
						}
					}
				}
				
				if (jcr.restrictions)
				{
					km.js.utils.forEachRestriction(jcr.restrictions, (function(jsti, jcr){
						
						return function(restr) {
							if (km.js.utils.isSimpleRestriction(restr) && !restr.property_id)
							{
								var baseType = km.js.utils.getTypeFromJSTI(jcr, jsti);
								restr.property_id = km.js.utils.nestedPropertyToPir(restr.property_name, baseType, jsti)
							}
						}
						
					})(jsti, jcr))
				}
				
			},
			
			isMeetCriteria: function(restriction, record) {
				if (restriction.operator.toLowerCase() === "eq")
				{
					return km.js.utils.propVal(record, restriction.property_id) === restriction.args[0];
				}
				else if (restriction.operator.toLowerCase() === "ne")
				{
					return km.js.utils.propVal(record, restriction.property_id) !== restriction.args[0];
				}
				else if (restriction.operator.toLowerCase() === "gt")
				{
					return km.js.utils.propVal(record, restriction.property_id) > restriction.args[0];
				}
				else if (restriction.operator.toLowerCase() === "ge")
				{
					return km.js.utils.propVal(record, restriction.property_id) >= restriction.args[0];
				}
				else if (restriction.operator.toLowerCase() === "lt")
				{
					return km.js.utils.propVal(record, restriction.property_id) < restriction.args[0];
				}
				else if (restriction.operator.toLowerCase() === "le")
				{
					return km.js.utils.propVal(record, restriction.property_id) <= restriction.args[0];
				}
				else if (restriction.operator.toLowerCase() === "ilike")
				{
					var val = km.js.utils.propVal(record, restriction.property_id);
					
					if (km.js.utils.isEmpty(val))
					{
						return false;
					}
					else
					{
						// convert both values to string and then to lower case
						var regexp = new RegExp(restriction.args[0].replace(/%/g, ".*"), "gi");
						//return (val + "").toLowerCase().indexOf((restriction.args[0] + "").toLowerCase()) > -1;
						return (val + "").toLowerCase().match(regexp) !== null;
					}
				}
				else if (restriction.operator.toLowerCase() === "not")
				{
					if (restriction.args === null || restriction.args.length === 0)
					{
						throw "Empty argument list for a NOT restriction";
					}
					else if (restriction.args.length > 1)
					{
						throw "More than one argument not allowed in a NOT restriction";
					}
					
					var arg = restriction.args[0];
					
					if (typeof arg === "object" && arg !== null)
					{
						return !this.isMeetCriteria(arg, record);
					}
					else
					{
						throw "Argument of a NOT restriction is not an object. Its value is " + arg;
					}
				}
				else if (restriction.operator.toLowerCase() === "or" || restriction.operator.toLowerCase() === "and")
				{
					if (restriction.args === null || restriction.args.length === 0)
					{
						throw "Empty argument list for a " + restriction.operator + " restriction";
					}
					
					if (restriction.operator.toLowerCase() === "and")
					{
						for (var i = 0; i < restriction.args.length; i++)
						{
							var arg = restriction.args[i];
							if (typeof arg === "object" && arg !== null)
							{
								// if any subcondition is not met, return false
								if (!this.isMeetCriteria(arg, record))
								{
									return false;
								}
							}
							else
							{
								throw "Argument of an AND restriction is not an object. Its value is " + arg;
							}
						}
						return true;
					}
					else if (restriction.operator.toLowerCase() === "or")
					{
						for (var i = 0; i < restriction.args.length; i++)
						{
							var arg = restriction.args[i];
							if (typeof arg === "object" && arg !== null)
							{
								// if any subcondition is met, return true
								if (this.isMeetCriteria(arg, record))
								{
									return true;
								}
							}
							else
							{
								throw "Argument of an AND restriction is not an object. Its value is " + arg;
							}
						}
						return false;
					}
				}
				else
				{
					console.log("Criteria not supported: " + restriction.operator);
					return false;
				}
			}
		}
		
		return ds;
	}
	// end create function
};