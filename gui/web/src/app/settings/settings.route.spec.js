/* jshint -W117, -W030 */
describe('settings routes', function () {
    describe('state', function () {
        var controller;
        var view = 'app/settings/settings.html';

        beforeEach(function() {
            module('app.settings', bard.fakeToastr);
            bard.inject('$httpBackend', '$location', '$rootScope', '$state', '$templateCache');
        });

        beforeEach(function() {
            $templateCache.put(view, '');
        });

        it('should map state settings to url /settings ', function() {
            expect($state.href('settings', {})).to.equal('/settings');
        });

        it('should map /settings route to settings View template', function () {
            expect($state.get('settings').templateUrl).to.equal(view);
        });

        it('of settings should work with $state.go', function () {
            $state.go('settings');
            $rootScope.$apply();
            expect($state.is('settings'));
        });
    });
});
