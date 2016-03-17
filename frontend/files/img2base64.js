var img2base64 = {
    sharpen: function (ctx, w, h, mix) {
        var weights = [0, -1, 0, -1, 5, -1, 0, -1, 0],
            katet = Math.round(Math.sqrt(weights.length)),
            half = (katet * 0.5) | 0,
            dstData = ctx.createImageData(w, h),
            dstBuff = dstData.data,
            srcBuff = ctx.getImageData(0, 0, w, h).data,
            y = h;
        while (y--) {
            x = w;
            while (x--) {
                var sy = y, sx = x, dstOff = (y * w + x) * 4, r = 0, g = 0, b = 0, a = 0;
                for (var cy = 0; cy < katet; cy++) {
                    for (var cx = 0; cx < katet; cx++) {

                        var scy = sy + cy - half;
                        var scx = sx + cx - half;

                        if (scy >= 0 && scy < h && scx >= 0 && scx < w) {

                            var srcOff = (scy * w + scx) * 4;
                            var wt = weights[cy * katet + cx];

                            r += srcBuff[srcOff] * wt;
                            g += srcBuff[srcOff + 1] * wt;
                            b += srcBuff[srcOff + 2] * wt;
                            a += srcBuff[srcOff + 3] * wt;
                        }
                    }
                }

                dstBuff[dstOff] = r * mix + srcBuff[dstOff] * (1 - mix);
                dstBuff[dstOff + 1] = g * mix + srcBuff[dstOff + 1] * (1 - mix);
                dstBuff[dstOff + 2] = b * mix + srcBuff[dstOff + 2] * (1 - mix)
                dstBuff[dstOff + 3] = srcBuff[dstOff + 3];
            }
        }

        ctx.putImageData(dstData, 0, 0);
    },
    drawImage: function (file, compress, imgType, imageScale, quality, sharpness, success, error) {
        var reader = new FileReader();
        reader.onerror = error;
        reader.onloadend = function () {
            var res = reader.result;
            if (!compress) {
                success(res);
                return;
            }
            var canvas = document.createElement("canvas");
            var ctx = canvas.getContext("2d");
            img = new Image();
            img.onerror = error;
            img.onload = function () {
                canvas.width = img.width;
                canvas.height = img.height;
                ctx.drawImage(img, 0, 0, img.width, img.height, 0, 0, img.width, img.height);
                img2base64.sharpen(ctx, img.width, img.height, sharpness / 100.0);
                var scale = 1.0 / (imageScale / 100.0);
                var shr = new Image();
                shr.onerror = error;
                shr.onload = function () {
                    canvas.width = img.width / scale;
                    canvas.height = img.height / scale;
                    ctx.drawImage(shr, 0, 0, img.width, img.height, 0, 0, img.width / scale, img.height / scale);
                    success(canvas.toDataURL(imgType, quality / 100.0));
                };
                shr.src = canvas.toDataURL();
            };
            img.src = res;
        };
        reader.readAsDataURL(file);
    }
};