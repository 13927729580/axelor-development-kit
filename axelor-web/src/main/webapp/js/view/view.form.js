/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
(function() {

var ui = angular.module('axelor.ui');

this.FormViewCtrl = FormViewCtrl;

FormViewCtrl.$inject = ['$scope', '$element'];
function FormViewCtrl($scope, $element) {

	DSViewCtrl('form', $scope, $element);

	var ds = $scope._dataSource;

	$scope.fields = {};
	$scope.fields_view = {};
	
	$scope.record = {};
	$scope.$$original = null;
	$scope.$$dirty = false;
	
	$scope.$events = {};
	
	/**
	 * Get field attributes.
	 * 
	 * @param field field name or field element
	 */
	$scope.getViewDef = function(field) {
		var name = id = field,
			elem = $(field),
			attrs = {};

		if (_.isObject(field)) {  // assume element
			name = elem.attr('x-field') || elem.attr('data-field') || elem.attr('name');
			id = elem.attr('x-for-widget') || elem.attr('id') || name;
		}
		
		attrs = _.extend({}, $scope.fields[name], $scope.fields_view[id]);

		return attrs;
	};

	$scope.doRead = function(id) {
		var params = {
			fields : _.pluck($scope.fields, 'name')
		};
		return ds.read(id, params);
	};

	function doEdit(id, dummy) {
		return $scope.doRead(id).success(function(record){
			if (dummy) {
				record = _.extend(dummy, record);
			}
			$scope.edit(record);
		});
	}
	
	var initialized = false;
	$scope.onShow = function(viewPromise) {
		
		var params = this._viewParams;
		var context = params.context || {};
		var recordId = params.recordId || context._showRecord;

		if (params.recordId) {
			params.recordId = undefined;
		}

		if (context._showRecord) {
			context._showRecord = undefined;
		}

		if (recordId) {
			return viewPromise.then(function(){
				var forceEdit = params.forceEdit || (params.params && params.params.forceEdit);
				doEdit(recordId);
				if (forceEdit) {
					$scope.setEditable();
				}
			});
		}
		
		if (!initialized && params.options && params.options.mode === "edit") {
			initialized = true;
			$scope._routeSearch = params.options.search;
			var recordId = +params.options.state;
			if (recordId > 0) {
				return viewPromise.then(function(){
					doEdit(recordId);
				});
			}
		}
		
		var page = ds.page(),
			record = null;
		
		if (page.index > -1) {
			record = ds.at(page.index);
		}
		viewPromise.then(function(){
			$scope.ajaxStop(function(){
				record = ($scope.record || {}).id ? $scope.record : record;
				if (record === undefined) {
					$scope.edit(null);
					return $scope.ajaxStop(function(){
						if (!$scope.record || !$scope.record.id) {
							$scope.$broadcast("on:new");
						}
					});
				}
				if (record && record.id)
					return doEdit(record.id);
				$scope.edit(record);
			});
		});
	};

	var editable = false;

	$scope.isEditable = function() {
		return editable;
	};

	$scope.setEditable = function() {
		editable = arguments.length === 1 ? _.first(arguments) : true;
	};
	
	$scope.getRouteOptions = function() {
		var rec = $scope.record,
			args = [];
		
		if (rec && rec.id > 0) {
			args.push(rec.id);
		}

		return {
			mode: 'edit',
			args: args,
			query: $scope._routeSearch
		};
	};
	
	$scope._routeSearch = {};
	$scope.setRouteOptions = function(options) {
		var opts = options || {},
			record = $scope.record || {},
			state = +opts.state || null;

		$scope._routeSearch = opts.search;
		if (record.id == state) {
			return $scope.updateRoute();
		}
		
		var params = $scope._viewParams;
		if (params.viewType !== "form") {
			return $scope.show();
		}
		return state ? doEdit(state) : $scope.edit(null);
	};

	$scope.edit = function(record) {
		$scope.editRecord(record);
		$scope.updateRoute();
		$scope._viewPromise.then(function(){
			$scope.ajaxStop(function(){
				var handler = $scope.$events.onLoad,
					record = $scope.record;
				if (handler && !_.isEmpty(record)) {
					setTimeout(handler);
				}
			});
		});
	};
	
	$scope.editRecord = function(record) {
		$scope.$$original = record || {};
		$scope.$$dirty = false;
		$scope.record = angular.copy($scope.$$original);
		$scope.ajaxStop(function(){
			$scope.$broadcast("on:edit", $scope.record);
			$scope.$broadcast("on:record-change", $scope.record);
		});
	};
	
	function updateSelected(name, records) {
		var path = $scope.formPath ? $scope.formPath + '.' + name : name,
			child = $element.find('[x-path="' + path + '"]:first'),
			childScope = child.data('$scope');
		if (records == null || childScope == null)
			return records;
		
		var selected = childScope.selection || [];
		
		return _.map(records, function(item, i) {
			return _.extend({}, item, {
				selected: _.contains(selected, i)
			});
		});
	}
	
	$scope.getContext = function() {
		var context = _.extend({}, $scope._routeSearch, $scope.record);
		if ($scope.$parent && $scope.$parent.getContext) {
			context._parent = $scope.$parent.getContext();
		}
		
		_.each($scope.fields, function(field, name) {
			if (/-many$/.test(field.type)) {
				var items = updateSelected(field.name, context[name]);
				if (items) {
					context[name] = items;
				}
			}
		});

		var dummy = $scope.getDummyValues();
		_.each(dummy, function (value, name) {
			if (value && value.$updatedValues) {
				dummy[name] = value.$updatedValues;
			}
			if (name.indexOf('$') === 0) {
				dummy[name.substring(1)] = dummy[name];
			}
		});
		
		context = _.extend(context, dummy);
		context._model = ds._model;
		return context;
	};

	$scope.isDirty = function() {
		return $scope.$$dirty = !ds.equals($scope.record, $scope.$$original);
	};

	$scope.$watch("record", function(rec, old) {
		var view = $scope.schema;
		if (view && view.readonlyIf) {
			var readonly = axelor.$eval($scope, view.readonlyIf, rec);
			if (_.isFunction($scope.attr)) {
				$scope.attr('readonly', readonly);
			}
			editable = !readonly;
		}
		if (rec === old) {
			return $scope.$$dirty = false;
		}
		$scope.$broadcast("on:record-change", rec);
		return $scope.$$dirty = $scope.isDirty();
	}, true);

	$scope.isValid = function() {
		return $scope.form && $scope.form.$valid;
	};

	$scope.canNew = function() {
		return $scope.hasButton('new');
	};

	$scope.canEdit = function() {
		return $scope.hasButton('edit');
	};
	
	$scope.canSave = function() {
		return $scope.hasButton('save') && $scope.$$dirty && $scope.isValid();
	};

	$scope.canDelete = function() {
		return $scope.hasButton('delete');
	};
	
	$scope.canCopy = function() {
		return !$scope.isEditable() && $scope.hasButton('copy') && !$scope.$$dirty && ($scope.record || {}).id;
	};
	
	$scope.canAttach = function() {
		return $scope.hasButton('attach');
	};

	$scope.canCancel = function() {
		return $scope.$$dirty;
	};

	$scope.canBack = function() {
		return !$scope.$$dirty;
	};

	$scope.onNew = function() {
		$scope.confirmDirty(function(){
			$scope.edit(null);
			$scope.setEditable();
			$scope.$broadcast("on:new");
		});
	};
	
	$scope.onNewPromise = null;
	$scope.defaultValues = null;
	
	$scope.$on("on:new", function onNewHandler(event) {
	
		function handleOnNew() {
			
			var handler = $scope.$events.onNew;
			var last = $scope.$parent.onNewPromise || $scope.onNewPromise;
			
			function reset() {
				$scope.onNewPromise = null;
			}
			
			function handle(defaults) {
				var promise = handler();
				if (promise && promise.then) {
					promise.then(reset, reset);
					promise = promise.then(function () {
						if ($scope.isDirty()) {
							var rec = _.extend({}, defaults, $scope.record);
							var res = $scope.editRecord(rec);
							if (rec && !rec.id) {
								rec._dirty = true;
							}
							return res;
						}
					});
				}
				return promise;
			}

			$scope.setEditable();

			if (handler && $scope.record) {
				if (last) {
					return $scope.onNewPromise = last.then(handle);
				}
				$scope.onNewPromise = handle($scope.defaultValues);
			} else if ($scope.defaultValues) {
				$scope.editRecord($scope.defaultValues);
			}
		}
		
		function afterVewLoaded() {
			if ($scope.defaultValues === null) {
				$scope.defaultValues = {};
				_.each($scope.fields, function (field, name) {
					if (field.defaultValue !== undefined) {
						$scope.defaultValues[name] = field.defaultValue;
					}
				});
			}
			return handleOnNew();
		}
		
		$scope._viewPromise.then(function() {
			$scope.$timeout(afterVewLoaded);
		});
	});
	
	$scope.$on("on:nav-click", function(event, tab) {
		var record, context, checkVersion;
		if (event.defaultPrevented || tab.$viewScope !== $scope) {
			return;
		}
		event.preventDefault();
		context = tab.context || {};
		record = $scope.record || {};
		checkVersion = "" + context.__check_version;

		if (!record.id || checkVersion !== "true") {
			return;
		}

		function compact(rec) {
			var res = {
				id: rec.id,
				version: rec.version || rec.$version
			};
			_.each(rec, function(v, k) {
				if (!v) return;
				if (v.id) res[k] = compact(v);
				if (_.isArray(v)) res[k] = _.map(v, compact);
			});
			return res;
		}

		ds.verify(compact(record)).success(function(res){
			if (res.status !== 0) {
				axelor.dialogs.confirm(
						_t("The record has been updated or delete by another action.") + "<br>" +
						_t("Would you like to reload the current record?"),
				function(confirmed){
					if (confirmed) {
						$scope.reload();
					}
				});
			}
		});
	});
	
	$scope.onEdit = function() {
		$.event.trigger('cancel:hot-edit');
		$scope.setEditable();
	};

	$scope.onCopy = function() {
		var record = $scope.record;
		ds.copy(record.id).success(function(record){
			$scope.edit(record);
			$scope.setEditable();
			record._dirty = true;
		});
	};
	
	$scope.getDummyValues = function() {
		if (!$scope.record) return {};
		var fields = _.keys($scope.fields);
		var extra = _.chain($scope.fields_view)
					  .filter(function(f){ return f.name && !_.contains(fields, f.name); })
					  .pluck('name')
					  .compact()
					  .value();
		return _.pick($scope.record, extra);
	};

	$scope.onSave = function(options) {
		
		var defer = $scope._defer();
		var event = $scope.$broadcast('on:before-save', $scope.record);
		var saveAction = $scope.$events.onSave;
		
		if (options && options.callOnSave === false) {
			saveAction = null;
		}

		if (event.defaultPrevented) {
			if (event.error) {
				axelor.dialogs.error(event.error);
			}
			setTimeout(function() {
				defer.reject(event.error);
			});
			return defer.promise;
		}

		function doSave() {
			var dummy = $scope.getDummyValues(),
				values = _.extend({}, $scope.record, (options||{}).values),
				promise;
			
			values = ds.diff(values, $scope.$$original);
			promise = ds.save(values).success(function(record, page) {
				return doEdit(record.id, dummy);
			});

			promise.success(function(record) {
				defer.resolve(record);
			});
			promise.error(function(error) {
				defer.reject(error);
			});
		}

		$scope.applyLater(function() {
			$scope.ajaxStop(function() {
				if (!$scope.canSave()) {
					$scope.showErrorNotice();
					return defer.promise;
				}
				if (saveAction) {
					return saveAction().then(doSave);
				}
				return doSave();
			});
		});
		return defer.promise;
	};
	
	$scope.confirmDirty = function(callback, cancelCallback) {
		var params = $scope._viewParams || {};
		if (!$scope.isDirty() || (params.params && params.params['show-confirm'] === false)) {
			return callback();
		}
		axelor.dialogs.confirm(_t("Current changes will be lost. Do you really want to proceed?"), function(confirmed){
			if (!confirmed) {
				if (cancelCallback) {
					cancelCallback();
				}
				return;
			}
			$scope.applyLater(callback);
		});
	};

	$scope.onBack = function() {
		var record = $scope.record || {};
		var editable = $scope.isEditable();

		if (record.id && editable) {
			$scope.setEditable(false);
			return;
		}

		$scope.switchTo("grid");
	};

	$scope.onRefresh = function() {
		$scope.confirmDirty($scope.reload);
	};

	$scope.reload = function() {
		var record = $scope.record;
		if (record && record.id) {
			return doEdit(record.id).success(function (rec) {
				var shared = ds.get(record.id);
				if (shared) {
					shared = _.extend(shared, rec);
				}
			});
		}
		$scope.edit(null);
		$scope.$broadcast("on:new");
	};

	$scope.onCancel = function() {
		var e = $scope.$broadcast("cancel:grid-edit");
		if (e.defaultPrevented) {
			return;
		}
		$scope.confirmDirty(function() {
			$scope.reload();
		});
	};
	
	var __switchTo = $scope.switchTo;
	
	$scope.switchTo = function(type, callback) {
		$scope.confirmDirty(function() {
			$scope.setEditable(false);
			$scope.editRecord(null);
			__switchTo(type, callback);
		});
	};

	$scope.onSearch = function() {
		var e = $scope.$broadcast("cancel:grid-edit");
		if (e.defaultPrevented) {
			return;
		}

		$scope.switchTo("grid");
	};
	
	$scope.pagerText = function() {
		var page = ds.page(),
			record = $scope.record || {};
			
		if (page && page.from !== undefined) {
			if (page.total == 0 || page.index == -1 || !record.id) return null;
			return _t("{0} of {1}", (page.from + page.index + 1), page.total);
		}
	},
	
	$scope.canNext = function() {
		var page = ds.page();
		return (page.index < page.size - 1) || (page.from + page.index < page.total - 1);
	};
	
	$scope.canPrev = function() {
		var page = ds.page();
		return page.index > 0 || ds.canPrev();
	};
	
	$scope.onNext = function() {
		$scope.confirmDirty(function() {
			ds.nextItem(function(record){
				if (record && record.id) doEdit(record.id);
			});
		});
	};
	
	$scope.onPrev = function() {
		$scope.confirmDirty(function() {
			ds.prevItem(function(record){
				if (record && record.id) doEdit(record.id);
			});
		});
	};

	function focusFirst() {
		$scope._viewPromise.then(function() {
			setTimeout(function() {
				$element.find('form :input:visible').not('[readonly],[type=checkbox]').first().focus().select();
			});
		});
	}
	
	function showLog() {
		
		var info = {};
			record = $scope.record || {};
		if (record.createdOn) {
			info.createdOn = moment(record.createdOn).format('DD/MM/YYYY HH:mm');
			info.createdBy = (record.createdBy || {}).name;
		}
		if (record.updatedOn) {
			info.updatedOn = moment(record.updatedOn).format('DD/MM/YYYY HH:mm');
			info.updatedBy = (record.updatedBy || {}).name;
		}
		var table = $("<table class='field-details'>");
		var tr;
		
		tr = $("<tr></tr>").appendTo(table);
		$("<th></th>").text(_t("Created On:")).appendTo(tr);
		$("<td></td>").text(info.createdOn).appendTo(tr);
		
		tr = $("<tr></tr>").appendTo(table);
		$("<th></th>").text(_t("Created By:")).appendTo(tr);
		$("<td></td>").text(info.createdBy).appendTo(tr);
		
		tr = $("<tr></tr>").appendTo(table);
		$("<th></th>").text(_t("Updated On:")).appendTo(tr);
		$("<td></td>").text(info.updatedOn).appendTo(tr);
		
		tr = $("<tr></tr>").appendTo(table);
		$("<th></th>").text(_t("Updated By:")).appendTo(tr);
		$("<td></td>").text(info.updatedBy).appendTo(tr);

		var text = $('<div>').append(table).html();

		axelor.dialogs.say(text);
	}
	
	$scope.toolmenu = [{
		icon: 'fa-gear',
		isButton: true,
		items: [{
			title: _t('Refresh'),
			click: function(e) {
				$scope.onRefresh();
			}
		}, {
			title: _t('Duplicate'),
			active: function () {
				return $scope.canCopy();
			},
			click: function(e) {
				$scope.onCopy();
			}
		}, {
		}, {
			title: _t('Log...'),
			active: function () {
				return $scope.hasAuditLog();
			},
			click: showLog
		}]
	}];

	$scope.onHotKey = function (e, action) {
		
		if (action === "save" && $scope.canSave()) {
			$scope.onSave();
		}
		if (action === "refresh") {
			$scope.onRefresh();
		}
		if (action === "new") {
			$scope.onNew();
		}
		if (action === "edit") {
			if ($scope.canEdit()) {
				$scope.onEdit();
			}
			focusFirst();
		}
		if (action === "select") {
			focusFirst();
		}
		if (action === "prev" && $scope.canPrev()) {
			$scope.onPrev();
		}
		if (action === "next" && $scope.canNext()) {
			$scope.onNext();
		}
		if (action === "search") {
			$scope.onBack();
		}
		
		$scope.applyLater();
		
		return false;
	};
};

ui.directive('uiViewForm', ['$compile', 'ViewService', function($compile, ViewService){
	
	function parse(scope, schema, fields) {
		
		var path = scope.formPath || "";
		
		function update(e, attrs) {
			_.each(attrs, function(v, k) {
				if (_.isUndefined(v)) return;
				e.attr(k, v);
			});
		}
		
		function process(items, parent) {
		
			$(items).each(function(){
			
				if (this.type == 'break') {
					return $('<br>').appendTo(parent);
				}
				
				if (this.type == 'field') {
					delete this.type;
				}
				
				var widget = this.widget,
					widgetAttrs = {},
					attrs = {};

				_.extend(attrs, this.widgetAttrs);

				_.each(this.widgetAttrs, function(value, key) {
					widgetAttrs['x-' + key] = value;
				});

				var item = $('<div></div>').appendTo(parent),
					field = fields[this.name] || {},
					widgetId = _.uniqueId('_formWidget'),
					type = widget;

				attrs = angular.extend(attrs, field, this);
				type = ui.getWidget(widget) || ui.getWidget(attrs.type) || attrs.type || 'string';

				if (_.isArray(attrs.selectionList) && !widget) {
					type = attrs.multiple ? 'multi-select' : 'select';
				}

				if (attrs.image ==  true) {
					type = "image";
				}
				if (type == 'label') { //TODO: allow <static> tag in xml view
					type = 'static';
				}

				attrs.serverType = attrs.type;
				attrs.type = type;
				
				item.attr('ui-' + type, '');
				item.attr('id', widgetId);
				
				scope.fields_view[widgetId] = attrs;

				//TODO: cover all attributes
				var _attrs = _.extend({}, attrs.attrs, this.attrs, widgetAttrs, {
						'name'			: attrs.name || this.name,
						'x-cols'		: this.cols,
						'x-colspan'		: this.colSpan,
						'x-rowspan'		: this.rowSpan,
						'x-widths'		: this.colWidths,
						'x-field'		: this.name,
						'x-title'		: attrs.title
					});

				if (attrs.showTitle !== undefined) {
					attrs.showTitle = attrs.showTitle !== false;
					_attrs['x-show-title'] = attrs.showTitle;
				}

				if (attrs.required)
					_attrs['ng-required'] = true;
				if (attrs.readonly)
					_attrs['x-readonly'] = true;
				
				if (_attrs.name) {
					_attrs['x-path'] = path ? path + "." + _attrs.name : _attrs.name;
				}
				
				update(item, _attrs);

				// enable actions & conditional expressions
				item.attr('ui-actions', '');
				item.attr('ui-widget-states', '');

				if (type == 'button' || type == 'static') {
					item.html(this.title);
				}
				
				if (/button|group|tabs|tab|separator|spacer|static/.test(type)) {
					item.attr('x-show-title', false);
				}
			
				var items = this.items || this.pages;
				if (items) {
					process(items, item);
					if (type != 'tabs') {
						item.attr('ui-table-layout', '');
					}
				}
				if (type === 'group' && _.all(items, function (x){ return x.type === 'button'; })) {
					item.addClass('button-group');
				}
			});
			return parent;
		}

		var elem = $('<form name="form" ui-form-gate ui-form ui-table-layout ui-actions ui-widget-states></form>');
		elem.attr('x-cols', schema.cols)
		  	.attr('x-widths', schema.colWidths);

		if (schema.css) {
			elem.addClass(schema.css);
		}
		
		process(schema.items, elem);
		
		return elem;
	}
	
	return function(scope, element, attrs) {
		
		scope.canShowAttachments = function() {
			return scope.canAttach() && (scope.record || {}).id;
		};
		
		scope.onShowAttachments = function(){
			var attachment = ViewService.compile('<div ui-attachment-popup></div>')(scope.$new());
			var popup = attachment.data('$scope');
			popup.show();
		};
		
		scope.hasAuditLog = function() {
			var record = scope.record || {};
			return record.createdOn || record.updatedOn || record.createBy || record.updatedBy;
		};

		scope.hasHelp = function() {
			var view = scope.schema;
			return view ? view.helpLink : false;
		};
		
		scope.hasWidth = function() {
			var view = scope.schema;
			return view && view.width;
		};

		scope.onShowHelp = function() {
			window.open(scope.schema.helpLink);
		};
		
		var translatted = null;
		
		scope.showErrorNotice = function () {
			var form = scope.form || $(element).data('$formController'),
				names;

			if (!form || form.$valid) {
				return;
			}

			var elems = element.find('[x-field].ng-invalid:not(fieldset)').filter(function() {
				var isInline = $(this).parents('.slickgrid').size() > 0;
				return !isInline || (isInline && $(this).is(':visible'));
			});
			var items = elems.map(function () {
				return {
					name: $(this).attr('x-field'),
					title: $(this).attr('x-title'),
				};
			});

			items = _.compact(items);

			if (items.length === 0) {
				return;
			}
			
			if (translatted == null) {
				translatted = {};
				_.each(scope.fields_view, function (v, k) {
					if (v.name) {
						translatted[v.name] = v.title;
					}
				});
			}

			items = _.map(items, function(item) {
				var value = item.title;
				if (item.name) {
					value = translatted[item.name] || value;
				}
				return '<li>' + value + '</li>';
			});

			items = '<ul>' + items.join('') + '</ul>';
			
			axelor.notify.error(items, {
				title: _t("The following fields are invalid:")
			});
		};

		var unwatch = scope.$watch('schema.loaded', function(viewLoaded){

			if (!viewLoaded) return;
			
			unwatch();

			var params = (scope._viewParams || {}).params || {};
			var schema = scope.schema;
			var form = parse(scope, schema, scope.fields);

			form = $compile(form)(scope);
			element.append(form);

			if (!scope._isPopup) {
				element.addClass('has-width');
			}

			var width = schema.width || params.width;
			if (width) {
				if (width === '100%' || width === '*') {
					element.removeClass('has-width');
				}
				form.css({
					width: width,
					minWidth: schema.minWidth || params.minWidth,
					maxWidth: schema.maxWidth || params.maxWidth
				});
			}
			
			if (scope._viewResolver) {
				scope._viewResolver.resolve(schema, element);
				scope.$broadcast("adjust:dialog");
			}
		});
	};
}]);

}).call(this);
