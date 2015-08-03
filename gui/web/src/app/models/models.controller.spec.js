/* jshint -W117, -W030 */
describe('ModelsController', function() {
    var controller;
    var models = mockData.getMockModels();

    beforeEach(function() {
        bard.appModule('app.models');
        bard.inject('eventHandler', '$httpBackend');
        /* global EventSource */
        eventHandler.eventSource = new EventSource('/_karma_/');
        $httpBackend.expectGET('/api/models').respond(200, models);
        bard.inject('rest', '$controller', '$log', '$rootScope', '$q', 'model');
        //sinon.stub(rest, 'getModels').returns($q.when(models));
        controller = $controller('ModelsController');
        $rootScope.$apply();
        $httpBackend.flush();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('ModelsController', function() {
        it('should be created successfully', function () {
            expect(controller).to.be.defined;
        });

        describe('after activate', function() {
            it('should have title of Models', function() {
                expect(controller.title).to.equal('Models');
            });

            it('should have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });

            it('should have at least 1 model', function () {
                expect(controller.models).to.have.length.above(0);
            });

            it('should have model count of 2', function () {
                expect(controller.models).to.have.length(2);
            });
        });

        describe('on a model creation event', function() {
            beforeEach(function() {
                var modelCreationEvent = mockData.getMockModelCreationEvent();
                eventHandler.eventSource.dispatchEvent(modelCreationEvent);
            });

            it('should result in a model count of 3', function () {
                expect(controller.models).to.have.length(3);
            });

            it('should log "Added"', function() {
                expect($log.info.logs).to.match(/Added/);
            });
        });

        describe('should after a model deletion event', function() {
            beforeEach(function() {
                var modelDeletionEvent = mockData.getMockModelDeletionEvent();
                eventHandler.eventSource.dispatchEvent(modelDeletionEvent);
            });

            it('have model count of 1', function () {
                expect(controller.models).to.have.length(1);
            });

            it('have a specific model left', function () {
                expect(controller.models[0].id).to.equal('5ccdc932-438d-4c45-b947-6dd208e31e31');
            });

            it('have logged "Removed"', function() {
                expect($log.info.logs).to.match(/Removed/);
            });
        });

        describe('should after a model update event', function() {
            beforeEach(function() {
                var modelUpdateEvent = mockData.getMockModelUpdateEvent();
                eventHandler.eventSource.dispatchEvent(modelUpdateEvent);
                this.oneModel = model.getModel('5ccdc932-438d-4c45-b947-6dd208e31e31');
            });

            it('still have model count of 2', function () {
                expect(controller.models).to.have.length(2);
            });

            it('have one of the model names updated', function () {
                expect(this.oneModel.name).to.equal('An updated name');
            });

            it('have one of the model versions bumped to 2', function () {
                expect(this.oneModel.version).to.equal(2);
            });

            it('have one of the model time stamps updated', function () {
                expect(this.oneModel.attributes.time).to.equal('2015-07-31T15:23:07.276+0200');
            });

            it('have logged "Updated"', function() {
                expect($log.info.logs).to.match(/Updated/);
            });
        });

        describe('should after a model creation request', function() {
            it('POST to correct URL', function () {
                $httpBackend.expectPOST('/api/models').respond(200);
                model.createModel('A new model');
                $httpBackend.flush();
            });

            it('POST a chosen model name to server', function() {
                var data = {'name': 'A new model'};
                $httpBackend.expectPOST(undefined, data).respond(200);
                model.createModel('A new model');
                $httpBackend.flush();
            });
        });

    });
});
