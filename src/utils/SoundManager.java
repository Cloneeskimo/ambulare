package utils;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.openal.ALC10.*;

/**
 * Provides methods to initialize and cleanup the sound device and context used to play sounds through the program
 */
public abstract class SoundManager {

    /**
     * Static Data
     */
    private static long device;  // the sound device used to play sounds
    private static long context; // the openAL context used to play sounds

    /**
     * Initializes the sound device and context. This is required for any sounds to play. This should be called at the
     * program's startup and at no other point (unless cleanup() is called the sound manager needs re-initialized)
     */
    public static void init() {
        device = alcOpenDevice((ByteBuffer) null); // open the sound device
        ALCCapabilities c = ALC.createCapabilities(device); // create the openAL capabilities
        context = alcCreateContext(device, (IntBuffer) null); // create the openAL context
        if (context == NULL) Utils.handleException(new Exception("Unable to create an AL context"),
                "utils.SoundManager", "init()", true); // if no context could be created, crash
        alcMakeContextCurrent(context); // make the created context the current context
        AL.createCapabilities(c); // create capabilities
        // log successful creation of sound manager
        Utils.log("SoundManager initialized", "utils.SoundManager", "init()", false);
    }

    /**
     * Cleans up the sound manager by destroying the openAL context and closing the device
     */
    public static void cleanup() {
        alcDestroyContext(context); // destroy the AL context
        alcCloseDevice(device); // close the AL device
        // log successful cleanup of sound manager
        Utils.log("SoundManager cleaned up", "utils.SoundManager", "cleanup()", false);
    }
}
