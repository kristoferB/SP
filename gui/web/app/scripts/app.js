'use strict';

/**
 * @ngdoc overview
 * @name spGuiApp
 * @description
 * # spGuiApp
 * hej
 * Main module of the application.
 */
angular
  .module('spGuiApp', [
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'ui.sortable',
    'jlareau.pnotify',
    'ui.bootstrap',
    'ngTextcomplete',
    'angularFileUpload',
    'gantt'
  ])
  .directive('input', ['$filter',
    function($filter) {
      return {
        require: '?ngModel',
        restrict: 'E',
        link: function(scope, element, attrs, ngModel) {
          if (!ngModel || attrs.type != "datetime-local") return;

          // Using AngularJS built in date filter, convert our date to RFC 3339
          function formatDateTime(value) {
            return value && angular.isDate(value)
              ? $filter('date')(value, "yyyy-MM-dd'T'HH:mm:ss")
              : '';
          }

          ngModel.$formatters.unshift(formatDateTime);

          // Convert the string value to Date object.
          function parseDateTime(value) {
            if(value && angular.isString(value)) {
              var date = new Date(value);
              date.setTime( date.getTime() + date.getTimezoneOffset()*60*1000 );
              return date;
            } else {
              return undefined;
            }
          }

          ngModel.$parsers.unshift(parseDateTime);
        }
      };
    }
  ])
  .config(function ($routeProvider, USER_ROLES) {
    $routeProvider
      /*.when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })*/
      /*.when('/runtime', {
        templateUrl: 'views/runtime.html',
        controller: 'RuntimeCtrl',
        data: [USER_ROLES.admin, USER_ROLES.editor]
      })*/
      .otherwise({
        redirectTo: '/'
      });
  })
  .config(function ($httpProvider) {
    $httpProvider.interceptors.push([
      '$injector',
      function ($injector) {
        return $injector.get('AuthInterceptor');
      }
    ]);
  })
  .config(['notificationServiceProvider', function(notificationServiceProvider) {
    notificationServiceProvider.setStack('bottom_right', 'stack-bottomright', {
      dir1: 'up',
      dir2: 'left',
      push: 'top'
    });

    notificationServiceProvider.setDefaultStack('bottom_right');
  }])
  .config(function($logProvider){
    $logProvider.debugEnabled(true);
  })
  /*.run(function ($rootScope, AUTH_EVENTS, AuthService) {
    $rootScope.$on('$routeChangeStart', function (event, next, current) {
      var authorizedRoles = next.data;
      console.log(authorizedRoles);
      if (!AuthService.isAuthorized(authorizedRoles)) {
        console.log("Trying to prevent default");
        event.preventDefault();
        if (AuthService.isAuthenticated()) {
          // user is not allowed
          console.log('The current user is not allowed to access this page.');
          $rootScope.$broadcast(AUTH_EVENTS.notAuthorized);
        } else {
          // user is not logged in
          console.log('No user is logged in.');
          $rootScope.$broadcast(AUTH_EVENTS.notAuthenticated);
        }
      }
      $rootScope.vars.isLoginPage = false;
    });
  })*/
  .constant('NAME_PATTERN', /^[A-Za-z0-9_-][A-Za-z0-9_-]*$/)
  .constant('ITEM_KINDS', ['Operation', 'Thing', 'SOPSpec', 'SPObject'])
  .constant('SV_KINDS', ['domain', 'range', 'boolean'])
  .run(function($rootScope, $location) {
    $rootScope.location = $location;
  })
  .config(["$httpProvider", function ($httpProvider) {
  $httpProvider.defaults.transformResponse.push(function(responseData){
    convertDateStringsToDates(responseData);
    return responseData;
  })
}]);

var regexIso8601 = /^(\d{4}|\+\d{6})(?:-(\d{2})(?:-(\d{2})(?:T(\d{2}):(\d{2}):(\d{2})\.(\d{1,})(Z|([\-+])(\d{2}):(\d{2}))?)?)?)?$/;

function convertDateStringsToDates(input) {
  // Ignore things that aren't objects.
  if (typeof input !== "object") return input;

  for (var key in input) {
    if (!input.hasOwnProperty(key)) continue;

    var value = input[key];
    var match;
    // Check for string properties which look like dates.
    if (typeof value === "string" && (match = value.match(regexIso8601))) {
      var milliseconds = Date.parse(match[0]);
      if (!isNaN(milliseconds)) {
        input[key] = new Date(milliseconds);
      }
    } else if (typeof value === "object") {
      // Recurse into object
      convertDateStringsToDates(value);
    }
  }
}

angular.module('spGuiApp').filter('filterElements', function () {
  return function (input) {
    var filteredInput ={};
    angular.forEach(input, function(value, key){
      if(key !== 'id' && key !=='name' && key !== 'isa' && key !== 'version' ){
        filteredInput[key]= value;
      }
    });
    return filteredInput;
  }});

angular.module('spGuiApp').filter('with', function() { // works on objects/maps like the built in 'filter' works on arrays
  return function(items, search) {
    var result = {};
    angular.forEach(items, function(item, id) {
      var valid = true;
      angular.forEach(search, function(value, key) {
        if(value instanceof Array) { // to enable accepting of alternative item key values, like isa: Thing || Operation
          var arrayValid = false;
          angular.forEach(value, function(element) {
            if (item.hasOwnProperty(key) && item[key] === element) {
              arrayValid = true;
            }
          });
          valid = arrayValid;
        } else {
          if (!item.hasOwnProperty(key) || item[key] !== value) {
            valid = false;
          }
        }
      });
      if(valid) {
        result[id] = item;
      }
    });
    return result;
  };
});
