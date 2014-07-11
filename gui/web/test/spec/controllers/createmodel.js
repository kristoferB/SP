'use strict';

describe('Controller: CreatemodelCtrl', function () {

  // load the controller's module
  beforeEach(module('spGuiApp'));

  var CreatemodelCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    CreatemodelCtrl = $controller('CreatemodelCtrl', {
      $scope: scope
    });
  }));


});
