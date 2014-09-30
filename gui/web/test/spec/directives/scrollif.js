'use strict';

describe('Directive: scrollIf', function () {

  // load the directive's module
  beforeEach(module('spGuiApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<scroll-if></scroll-if>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the scrollIf directive');
  }));
});
