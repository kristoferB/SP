describe('Nav', function () {

  beforeEach(function () {
    browser.get('/');
  });

  it('should have <nav>', function () {
    expect(element(by.css('sp-app sp-nav')).isPresent()).toEqual(true);
  });

});
