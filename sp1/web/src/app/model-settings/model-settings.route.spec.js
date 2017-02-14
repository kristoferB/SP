/* jshint -W117, -W030 */
describe('model settings routes', function () {
    describe('state', function () {
        var view = 'app/model-settings/model-settings.html';

        beforeEach(function() {
            module('app.model-settings', bard.fakeToastr);
            bard.inject('$rootScope', '$state', '$templateCache');
        });

        beforeEach(function() {
            $templateCache.put(view, '');
        });

        it('should map state settings to url /settings ', function() {
            expect($state.href('model-settings', {})).to.equal('/model-settings');
        });

        it('should map /model-settings route to settings view template', function () {
            expect($state.get('model-settings').templateUrl).to.equal(view);
        });

        it('of model-settings should work with $state.go', function () {
            $state.go('model-settings');
            $rootScope.$apply();
            expect($state.is('model-settings'));
        });
    });
});
