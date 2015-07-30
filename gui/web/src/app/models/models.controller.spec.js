/* jshint -W117, -W030 */
describe('ModelsController', function() {
    var controller;
    var models = mockData.getMockModels();
    var modelCreationEvent = mockData.getMockModelCreationEvent();

    beforeEach(function() {
        bard.appModule('app.models');
        bard.inject('rest', 'eventHandler', '$controller', '$log', '$rootScope', '$q', '$httpBackend');
    });

    beforeEach(function () {
        sinon.stub(rest, 'getModels').returns($q.when(models));
        controller = $controller('ModelsController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('Models controller', function() {
        it('should be created successfully', function () {
            expect(controller).to.be.defined;
        });

        describe('should after activate', function() {
            it('have title of Models', function() {
                expect(controller.title).to.equal('Models');
            });

            it('have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });

            it('have at least 1 model', function () {
                expect(controller.models).to.have.length.above(0);
            });

            it('have model count of 2', function () {
                expect(controller.models).to.have.length(2);
            });
        });

        describe('should after model creation', function() {
            beforeEach(function() {
                eventHandler.eventSource.dispatchEvent(modelCreationEvent);
            });

            it('have model count of 3', function () {
                expect(controller.models).to.have.length(3);
            });
        });

    });
});
