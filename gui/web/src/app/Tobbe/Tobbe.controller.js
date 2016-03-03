/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.Tobbe')
      .controller('TobbeController', TobbeController);

    TobbeController.$inject = ['$scope', 'dashboardService', 'eventService','spServicesService'];
    /* @ngInject */
    function TobbeController($scope, dashboardService, eventService,spServicesService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what


        vm.a = 0;
        vm.b = 0;
        vm.result = 0;

        vm.bigR = 255;
        vm.$var = 0;
        vm.resultColor = '#00FF00';

        //functions
        vm.getProperColor = getProperColor;
        vm.saveNumber = saveNumber;
        vm.reset = reset;
        vm.build = build;
        vm.tryTheTower = tryTheTower;
        vm.calc = calc;

        //Contains the colours of the cubes
        vm.ButtonColour = {
            kub: [
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0],
                [0, 0, 0, 0]
            ]
        };

        //Remove these when done
        vm.debug = 0;
        vm.debug2 = 0;

        activate();

        function activate() {
            $scope.$on('closeRequest', function () {
                dashboardService.closeWidget(vm.widget.id);
            });
        }

        function calc(sign) {
            //spServicesService.callService();
            var mess = {"data": {"a": vm.a, "b": vm.b, "sign": sign}};

            spServicesService.callService(spServicesService.getService("TobbeG"),
                mess,
                function (resp) {
                    if (_.has(resp, 'attributes.result')) {
                        vm.result = resp.attributes.result;
                    }
                }
            )
        }

        /**
         * Change the colour of a button.
         * @param btn
         * @param row
         * @param column
         * @param reset
         */
        function setColor(btn, row, column, reset) {
            var property = document.getElementById(btn);

            switch (vm.ButtonColour.kub[row][column]) {
                case 0: //YELLOW = 1
                    property.style.backgroundColor = "#ffff1a";
                    break;
                case 1://GREEN = 2
                    property.style.backgroundColor = "#00cc00";
                    break;
                case 2://RED = 3
                    property.style.backgroundColor = "#e60000";
                    break;
                case 3://BLUE = 4
                    property.style.backgroundColor = "#0080ff";
                    break;
                default:
                    property.style.backgroundColor = "#FFFFFF";
            }

            vm.ButtonColour.kub[row][column] = ++vm.ButtonColour.kub[row][column] % 5;

            if (reset) {
                property.style.backgroundColor = "#FFFFFF"
                vm.ButtonColour.kub[row][column] = 0;
            }
        }

        /**
         * Denna funktionen kör setColor functionen.
         * Om det inte behövs köras något mer kan den
         * tas bort och spridas ut bland knapparna.
         * @param row
         * @param column
         */
        function saveNumber(row, column) {
            //setColor('button' + number, eval('vm.button' + number), 0);
            //setColor('button' + number, eval('vm.ButtonColour.kub[' + row + '][' + column + ']'), 0);
            setColor('button' + eval(row * 10 + column + 11), row, column, 0);
            tryTheTower(0);
            vm.debug = row;
            vm.debug2 = column;
        }

        /**
         * Resets the colour of every button.
         */
        function reset() {
            for (var i = 0; i < 4; i++) {
                for (var j = 0; j < 4; j++) {
                    setColor('button' + eval(i * 10 + j + 11), i, j, 1);
                }
            }
            var property = document.getElementById('buttonBuild');
            property.style.backgroundColor = "#ffffff";
        }

        /**
         * Does not work!
         */
        function getProperColor() {
            if ($var > 0 && $var <= 5)
                vm.resultColor = '#00FF00';
            else if ($var >= 6 && $var <= 10)
                vm.resultColor = '#FF8000';
            else if ($var >= 11)
                vm.resultColor = '#FF0000';
        }

        /**
         * work in progress
         */
        function build() {
            //alert('hej');
        }

        /**
         * Checks if the tower is allowed to be built and
         * updates the build-button colour accordingly.
         * It also displays some messages.
         * @param display
         */
        function tryTheTower(display) {
            var property = document.getElementById('buttonBuild');
            var shallNotPass = 0;
            var anyCubesAtAll = 0;

            for (var column = 0; column < 4; column++) {
                var temp = 0;
                for (var row = 3; row > -1; row--) {
                    if (temp && !vm.ButtonColour.kub[row][column]) {
                        shallNotPass = 1;
                    }
                    if (vm.ButtonColour.kub[row][column]) {
                        temp = 1;
                        anyCubesAtAll = 1;
                    }
                }
            }
            if(shallNotPass)
                property.style.backgroundColor = "#ff3333";
            else if(anyCubesAtAll)
                property.style.backgroundColor = "#66ff66";
            else
                property.style.backgroundColor = "#ffffff";

            if(display) {
                if (shallNotPass)
                    alert('Your Tower Shall Not Pass!');
                else if(anyCubesAtAll)
                    alert('OK');
                else
                    alert('You Need to Choose at Least One');
            }
        }
    }
})();
