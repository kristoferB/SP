var app = angular.module("myApp", ["ngRoute"]);
app.config(function($routeProvider) {
    $routeProvider
    .when("/", {
        templateUrl : "ng1things/main.htm"
    })
    .when("/dogs", {
        templateUrl : "ng1things/dogs.htm",
        controller : "myCtrl"
    })
    .when("/cats", {
        templateUrl : "ng1things/cats.htm"
    });
});
