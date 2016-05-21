package nesdroid

import (
    //"unsafe"
    "encoding/binary"
	//"golang.org/x/mobile/exp/f32"
    //"log"
)

type vid struct {
	pixelBuffer chan []uint32
}

var video vid

func GetPixelBuffer() []byte {
	if video.pixelBuffer != nil {
		frame := <-video.pixelBuffer

		//return (*[]byte)(unsafe.Pointer(&frame))[:]

        buf := make([]byte, len(frame) * 4)
        //log.Printf("frame[%v], bufz[%v]", len(frame), len(buf))

        //buf := make([]float32, len(frame))
        i := 0
		for k, v := range frame {
            i = k * 4
            //log.Printf("k = %v, v = %v, [%v:%v]", k, v, i, i + 4)
            binary.LittleEndian.PutUint32(buf[i:i+4], v)
			//buf[k] = float32(v)
		}

        return buf
		//return f32.Bytes(binary.LittleEndian, buf...)
	}
	return nil
}
