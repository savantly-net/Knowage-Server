/*
Knowage, Open Source Business Intelligence suite
Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

Knowage is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

Knowage is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author Marco Balestri <marco.balestri@eng.it>
 */

(function () {
	angular
		.module('cockpitModule')
		.directive('cockpitPythonWidget', function () {
			return {
				templateUrl: baseScriptPath+ '/directives/cockpit-widget/widget/pythonWidget/templates/pythonWidgetTemplate.html',
				controller: cockpitPythonWidgetControllerFunction,
				compile: function (tElement, tAttrs, transclude) {
					return {
						post: function postLink(scope, element, attrs, ctrl, transclud) {
							element.ready(function () {
							scope.initWidget();
							});
						}
					};
				}
			}
		})

		.directive('bindHtmlCompile', ['$compile', function ($compile) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                scope.$watch(function () {
                    return scope.$eval(attrs.bindHtmlCompile);
                }, function (value) {
                    element.html(value && value.toString());
                    var compileScope = scope;
                    if (attrs.bindHtmlScope) {
                        compileScope = scope.$eval(attrs.bindHtmlScope);
                    }
                    $compile(element.contents())(compileScope);
                });
            }
        };
    }])

	function cockpitPythonWidgetControllerFunction(
			$scope,
			$mdDialog,
			$mdPanel,
			$q,
			$timeout,
			$http,
			$sce,
			sbiModule_translate,
			cockpitModule_properties,
			cockpitModule_generalServices,
			cockpitModule_datasetServices) {

		$scope.getTemplateUrl = function (template) {
	  		return cockpitModule_generalServices.getTemplateUrl('pythonWidget', template);
	  	}

		$scope.refresh = function (element, width, height, datasetRecords, nature) {
			$scope.showWidgetSpinner();
			if(nature == 'init') {
				$timeout(function () {
					$scope.widgetIsInit = true;
					cockpitModule_properties.INITIALIZED_WIDGETS.push($scope.ngModel.id);
				}, 500);
			}
			$scope.sendData();
			$scope.hideWidgetSpinner();
		}

		$scope.reinit = function() {
			$scope.refreshWidget();
		}

		$scope.createIframe = function () {
			//var element = document.getElementById('bokeh');
			var element = angular.element( document.querySelector( '#w' + $scope.ngModel.id + ' #bokeh' ) );
			var iframe = document.createElement('iframe');
			iframe.id = "bokeh_" + $scope.ngModel.id;
			iframe.classList.add("layout-fill");
			element.append(iframe);
			document.getElementById(iframe.id).contentWindow.document.open();
			document.getElementById(iframe.id).contentWindow.document.write($scope.pythonOutput);
			document.getElementById(iframe.id).contentWindow.document.close();
		}

		$scope.sendData = function () {
			url_string = window.location.href
			var url = new URL(url_string);
			var encodedUserId = url.searchParams.get("user_id");
			if ($scope.ngModel.dataset != undefined) {
				var dataset = cockpitModule_datasetServices.getDatasetById($scope.ngModel.dataset.dsId);
				dataset_name = dataset.label
			}
			else {
				dataset_name = "" //no dataset selected
			}
		    $http({
		        url: 'http://localhost:5000/' + $scope.ngModel.pythonOutputType,
		        method: "POST",
		        headers: {'Content-Type': 'application/json',
		        		  'Authorization': encodedUserId,
		        		  'Dataset-name': dataset_name},
		        data: { 'script' : $scope.ngModel.pythonCode,
		        		'output_variable' : $scope.ngModel.pythonOutput,
		        		'widget_id' :  $scope.ngModel.id }
		    })
		    .then(function(response) {
		            $scope.pythonOutput = $sce.trustAsHtml(response.data);
		            if ($scope.ngModel.pythonOutputType == 'bokeh') {
						$scope.createIframe();
					}
		    },
		    function(response) { // todo
		            // failed
		    });
		}

		$scope.init = function (element, width, height) {
			$scope.showWidgetSpinner();
			$scope.refresh(element, width, height, null, 'init');
		}

		$scope.getOptions = function () {
			var obj = {};
			obj["page"] = 0;
			obj["itemPerPage"] = 10;
			obj["type"] = 'python';
			return obj;
		}

		$scope.editWidget = function (index) {
			var finishEdit=$q.defer();
			var config = {
				attachTo:  angular.element(document.body),
				controller: pythonWidgetEditControllerFunction,
				disableParentScroll: true,
				templateUrl: $scope.getTemplateUrl('pythonWidgetEditPropertyTemplate'),
				position: $mdPanel.newPanelPosition().absolute().center(),
				fullscreen :true,
				hasBackdrop: true,
				clickOutsideToClose: false,
				escapeToClose: false,
				focusOnOpen: true,
				preserveScope: false,
				locals: {finishEdit: finishEdit, model: $scope.ngModel},
			};
			$mdPanel.open(config);
			return finishEdit.promise;
		}

	}

	/**
	 * register the widget in the cockpitModule_widgetConfigurator factory
	 */
	addWidgetFunctionality("python", {'initialDimension': {'width':5, 'height':5}, 'updatable':true, 'clickable':true});

})();
