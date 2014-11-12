'use strict';

describe('Controller: ModelhistoryCtrl', function () {

  // load the controller's module
  beforeEach(module('spGuiApp'));

  var ModelhistoryCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    ModelhistoryCtrl = $controller('ModelhistoryCtrl', {
      $scope: scope
    });
  }));


});
