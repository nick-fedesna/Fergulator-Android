package nesdroid

import (
    "encoding/binary"
)

type vid struct {
    pixelBuffer chan []uint32
}

var video vid
var buffer []byte

func GetPixelBuffer() []byte {
    if video.pixelBuffer != nil {

        //start := time.Now()
        frame := <-video.pixelBuffer
        //elapsed := time.Since(start)
        //log.Printf("channel took %s", elapsed)

        if buffer == nil {
            buffer = make([]byte, len(frame) * 4)
        }

        i := 0
        for k, v := range frame {
            i = k * 4
            binary.BigEndian.PutUint32(buffer[i:i + 4], v)
        }

        return buffer

    }
    return nil
}
