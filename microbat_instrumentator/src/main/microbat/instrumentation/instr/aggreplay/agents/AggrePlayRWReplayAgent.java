package microbat.instrumentation.instr.aggreplay.agents;

/**
 * Different mode of replay, used to test stricter replay.
 * Use blocking yield instead.
 * For every shm, we have a list of writes, and the reads associated to each write.
 * So each write, we can simply wait for the read to occur
 * before we allow another write.
 * 
 * Idg why aggreplay has  that information but doesn't use it.
 * I'm not sure if this introduces deadlocks.
 * @author Gabau
 *
 */
public class AggrePlayRWReplayAgent extends AggrePlayRecordingAgent {

}
