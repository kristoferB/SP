describe('App', function () {

  beforeEach(function () {
    browser.get('/');
  });

  it('should have a title', function () {
    expect(browser.getTitle()).toEqual("Sequence Planner GUI");
  });

  it('should have <sp-nav>', function () {
    expect(element(by.css('sp-app sp-nav')).isPresent()).toEqual(true);
  });

  it('should have <router-outlet>', function () {
    expect(element(by.css('sp-app router-outlet')).isPresent()).toEqual(true);
  });

  //it('should have <footer>', function () {
  //  expect(element(by.css('sp-app footer')).getText()).toEqual("Webpack Angular 2 Starter");
  //});

});
