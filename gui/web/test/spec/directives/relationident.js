'use strict';

describe('Directive: relationident', function () {

  // load the directive's module
  beforeEach(module('spGuiApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<relationident></relationident>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the relationident directive');
  }));
});
