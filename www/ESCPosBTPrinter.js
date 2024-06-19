var exec = require('cordova/exec');

var ESCPosBTPrinter = {
    connect: function(address, success, error) {
        exec(success, error, 'ESCPosBTPrinter', 'connect', [address]);
    },
    print: function(text, success, error) {
        exec(success, error, 'ESCPosBTPrinter', 'print', [text]);
    },
    disconnect: function(success, error) {
        exec(success, error, 'ESCPosBTPrinter', 'disconnect');
    },
    printTemplate: function(success, error) {
        exec(success, error, 'ESCPosBTPrinter', 'printTemplate');
    },
    printWhitFormat: function (config, success, error) {
        cordova.exec(success, error, 'ESCPosBTPrinter', 'printWhitFormat', [config]);
    },
    printBarCode: function (config, success, error) {
        cordova.exec(success, error, 'ESCPosBTPrinter', 'printBarCode', [config]);
    },
    printImage: function (config, success, error) {
        cordova.exec(success, error, 'ESCPosBTPrinter', 'printImage', [config]);
    },
    getConnectState: function(success, error) {
        exec(success, error, 'ESCPosBTPrinter', 'getConnectState');
    },
};

module.exports = ESCPosBTPrinter;
