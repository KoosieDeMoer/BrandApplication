'use strict';
var module = angular.module("MainModule", []);

module.directive('fileModel', ['$parse', function ($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            var modelSetter = model.assign;
            
            element.bind('change', function(){
                scope.$apply(function(){
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
}]);

module.service('fileUpload', ['$http', function ($http) {
}]);

module.controller("MainController", [ '$scope', '$http', '$window', 'fileUpload', function($scope, $http, $window) {

	$scope.brand_info = null;
	$scope.brands = null;
	$scope.images = null;
	
	_refreshPage();

	$scope.loadBrand = function() {
		
		document.body.style.cursor = 'wait';
		$http({
			method : 'GET',
			url : '/Branding/info?brand=' + $scope.newDocForm.brandToLoad
		}).then(function successCallback(response) {
			$scope.brand_info = angular.fromJson(response.data);
			$scope.newDocForm.brand = $scope.newDocForm.brandToLoad;
			document.body.style.cursor = 'default';

		}, function errorCallback(response) {
			_error(response);
		});

	}

	$scope.viewBrand = function() {
		
		document.body.style.cursor = 'wait';

		if($scope.newDocForm.brand == "") {
			$scope.loadBrand();
		}
		var windowId = '_branding';
		if ($window[windowId] && !$window[windowId].closed) {
//			$window[windowId].location.reload(true); 
			$window[windowId].focus();
		} else {
			$window[windowId] = $window.open('/brandablePage.html', '_branding', 'left=1100,top=550,height=400,width=600,scrollbars=yes,status=yes');
		}

		document.body.style.cursor = 'default';
	}

	$scope.createBrandingInfo = function() {
		document.body.style.cursor = 'wait';
		$scope.newDocForm.created = '';
		$http({
			method : 'PUT',
			url : '/Branding/info?brand=' + $scope.newDocForm.brand,
			data : $scope.brand_info
		}).then(function successCallback(response) {
			$scope.newDocForm.created = 'Brand ' + $scope.newDocForm.brand + ' created';
			_updateBrandsList();

		}, function errorCallback(response) {
			_error(response);
		});

	};

    $scope.uploadFile = function(){
        var file = $scope.myFile;
        var uploadUrl = "/Branding/upload";
        _uploadFileToUrl(file, uploadUrl);
    };

    $scope.displayFile = function(){
    	document.getElementById('image').src = '/Branding/download/' + $scope.newDocForm.fileName;
    };

    $scope.newDocForm = {
			brand : "",
			created : ""
		};
	/* Private Methods */

	function _refreshPage() {

		_updateBrandsList();
		_updateImageList();
	}

	function _updateBrandsList() {

		document.body.style.cursor = 'wait';
		$http({
			method : 'GET',
			url : '/Branding/infos'
		}).then(function successCallback(response) {
			$scope.brands = response.data;
			document.body.style.cursor = 'default';

		}, function errorCallback(response) {
			_error(response);
		});

	}
	
	function _updateImageList() {

		document.body.style.cursor = 'wait';
		$http({
			method : 'GET',
			url : '/Branding/images'
		}).then(function successCallback(response) {
			$scope.images = response.data;
			document.body.style.cursor = 'default';

		}, function errorCallback(response) {
			_error(response);
		});

	}

	function _uploadFileToUrl(file, uploadUrl){
        var fd = new FormData();
        fd.append('file', file);
        $http.post(uploadUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        })
        .success(function(){
        	_updateImageList();
        })
        .error(function(){
        	_error(response);
        });
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
}]);


