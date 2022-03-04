const fs = require("fs");
const path = require("path");

module.exports = function(context) {
    function getPlatformVersion(context) {
        var projectRoot = context.opts.projectRoot;
    
        var packageJsonFile = path.join(
            projectRoot,
            "package.json"
        );
    
        var devDependencies = require(packageJsonFile).devDependencies;
    
        if(devDependencies !== undefined){
            //Probably MABS7
            var platform = devDependencies["cordova-android"];
            if (platform.includes('^')){
                var index = platform.indexOf('^');
                platform = platform.slice(0, index) + platform.slice(index+1);
            }
            if (platform.includes('#')){
                var index = platform.indexOf('#');
                platform = platform.slice(index+1);
            }
            if (platform.includes('+')){
                var index = platform.indexOf('+');
                platform = platform.slice(0,index);
            }
            return platform;
        } else {
            //Probably MABS6.X
            var platformsJsonFile = path.join(
                projectRoot,
                "platforms",
                "platforms.json"
            );
            var platforms = require(platformsJsonFile);
            var platform = context.opts.plugin.platform;
            return platforms[platform];
        }    
    }
    const androidVersion = parseInt(getPlatformVersion(context));
    if(androidVersion == 10){
        var pathConfig = path.join(
            context.opts.projectRoot,
            "platforms",
            "android",
            "cdv-gradle-config.json"
        );
        /*var pathConfigXML = path.join(
            context.opts.projectRoot,
            "platforms",
            "android",
            "cdv-gradle-config.json"
        );*/
        var pathAndroid = path.join(
            context.opts.projectRoot,
            "platforms",
            "android",
            "android.json"
        );
        var content1 = fs.readFileSync(pathConfig,"utf-8");
        content1 = content1.replace("1.3.50","1.5.21");
        fs.writeFileSync(pathConfig,content1);

        /*
        var content1 = fs.readFileSync(pathConfigXML,"utf-8");
        content1 = content1.replace("1.3.50","1.5.21");
        fs.writeFileSync(pathConfigXML,content1);*/

        var content1 = fs.readFileSync(pathAndroid,"utf-8");
        content1 = content1.replace("1.3.50","1.5.21");
        fs.writeFileSync(pathAndroid,content1);
        console.log(androidVersion)
    }else if (androidVersion<10){
        console.log(androidVersion)
        var pathConfig = path.join(
            context.opts.projectRoot,
            "platforms",
            "android",
            "build.gradle"
        );
        var pathAndroid = path.join(
            context.opts.projectRoot,
            "platforms",
            "android",
            "android.json"
        );
        var content1 = fs.readFileSync(pathConfig,"utf-8");
        content1 = content1.replace("1.3.50","1.5.21");
        fs.writeFileSync(pathConfig,content1);

        var content1 = fs.readFileSync(pathAndroid,"utf-8");
        content1 = content1.replace("1.3.50","1.5.21");
        fs.writeFileSync(pathAndroid,content1);
        console.log(androidVersion)
    }
};