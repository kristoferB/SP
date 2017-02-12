/* jshint -W117, -W030 */
describe('models routes', function () {
    describe('state', function () {
        var controller;
        var view = 'app/models/models.html';

        beforeEach(function() {
            module('app.models', bard.fakeToastr);
            bard.inject('$httpBackend', '$location', '$rootScope', '$state', '$templateCache');
        });

        beforeEach(function() {
            $templateCache.put(view, '');
        });

        it('should map state settings to url /models ', function() {
            expect($state.href('models', {})).to.equal('/models');
        });

        it('should map /models route to models View template', function () {
            expect($state.get('models').templateUrl).to.equal(view);
        });

        it('of models should work with $state.go', function () {
            $state.go('models');
            $rootScope.$apply();
            expect($state.is('models'));
        });
    });
});
