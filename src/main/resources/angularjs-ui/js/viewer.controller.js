'use strict';
var module = angular.module("ViewerModule", []);

module.controller("ViewerController", [ '$scope', '$http', '$window',
		function($scope, $http, $window) {

			_refreshPage();

			/* Private Methods */

			function _refreshPage() {

				_registerEventSourceListener();
			}

			function _registerEventSourceListener() {
				var feed = new EventSource('/event-source');
				var handler = function(event) {
					$scope.$apply(function() {
						// console.log('from event source:' + event.data);
						// simply refresh when a signal arrives
						$window.location.reload(true);
					});

				}
				feed.addEventListener('message', handler, false);
			}

			function _success(response) {
				$scope.errorMessage = false;
				document.body.style.cursor = 'default';
			}

			function _error(response) {
				$scope.errorMessage = response.data;
				document.body.style.cursor = 'default';
			}
			;
		} ]);
